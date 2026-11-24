package com.nfcpay.simulator.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import com.nfcpay.simulator.domain.model.CardPublicMetadata
import com.nfcpay.simulator.domain.model.NfcReadResult
import java.io.IOException

/**
 * Standard read-only NFC contactless public data reader.
 *
 * Security & Compliance guarantees:
 * - Strictly read-only EMV public discovery (SELECT PPSE, SELECT AID, GPO, READ RECORD).
 * - NEVER requests payment cryptograms (no GENERATE AC).
 * - NEVER performs authentication, PIN verification, key guessing, or bypass.
 * - NEVER logs APDU payloads, TLV bytes, or card digits.
 * - Does not claim guaranteed physical memory zeroing of the JVM runtime,
 *   while still clearing transient byte arrays as good hygiene.
 */
class EmvPublicDataReader {

    companion object {
        private const val ISO_DEP_TIMEOUT_MS = 2500
        private const val MAX_RECORDS_TO_READ = 6

        // 2PAY.SYS.DDF01 (Standard Contactless Payment System Environment)
        private val SELECT_PPSE_APDU = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x0E,
            0x32, 0x50, 0x41, 0x59, 0x2E, 0x53, 0x59,
            0x53, 0x2E, 0x44, 0x44, 0x46, 0x30, 0x31,
            0x00
        )

        // Common public contactless EMV AIDs as fallback if PPSE returns no directory entries
        private val FALLBACK_PUBLIC_AIDS = listOf(
            byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x06, 0x58, 0x10, 0x10), // MIR
            byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x10, 0x10), // Visa
            byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x10, 0x10), // Mastercard
            byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x30, 0x60), // Maestro
            byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x03, 0x33, 0x01, 0x01)  // UnionPay
        )
    }

    fun readTag(tag: Tag): NfcReadResult {
        val techList = tag.techList ?: emptyArray()
        if (techList.isEmpty()) {
            return NfcReadResult.UnsupportedCard
        }

        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            return readIsoDepCard(isoDep)
        }

        val knownSimpleTech = techList
            .map { it.substringAfterLast('.') }
            .firstOrNull { it in setOf("NfcA", "NfcB", "NfcF", "NfcV") }

        return if (knownSimpleTech != null) {
            NfcReadResult.NumberUnavailable(
                CardPublicMetadata(
                    last4Digits = null,
                    applicationLabel = null,
                    nfcTechnology = knownSimpleTech,
                    isNoCardDemo = false
                )
            )
        } else {
            NfcReadResult.UnsupportedCard
        }
    }

    private fun readIsoDepCard(isoDep: IsoDep): NfcReadResult {
        var appLabel: String? = null
        var last4: String? = null

        try {
            isoDep.connect()
            isoDep.timeout = ISO_DEP_TIMEOUT_MS

            // Step 1: SELECT PPSE (read-only directory discovery)
            val candidateAids = mutableListOf<ByteArray>()
            val ppseResp = transceiveAndCheckOk(isoDep, SELECT_PPSE_APDU)
            if (ppseResp != null) {
                val ppseNodes = BerTlvParser.parse(ppseResp)
                ppseResp.fill(0)
                if (appLabel == null) {
                    appLabel = BerTlvParser.extractApplicationLabel(ppseNodes)
                }
                for (aid in BerTlvParser.findAllByTag(ppseNodes, "4F")) {
                    if (aid.size in 5..16) {
                        candidateAids.add(aid.copyOf())
                    }
                }
                BerTlvParser.wipeNodes(ppseNodes)
            }

            if (candidateAids.isEmpty()) {
                candidateAids.addAll(FALLBACK_PUBLIC_AIDS)
            }

            // Step 2: Try selecting the first responding public application AID
            for (aid in candidateAids.take(3)) {
                val selectAidApdu = buildSelectAidApdu(aid)
                val selectResp = transceiveAndCheckOk(isoDep, selectAidApdu) ?: continue
                val selectNodes = BerTlvParser.parse(selectResp)
                selectResp.fill(0)

                val selectedLabel = BerTlvParser.extractApplicationLabel(selectNodes)
                if (!selectedLabel.isNullOrEmpty()) {
                    appLabel = selectedLabel
                }

                // Check if public last 4 digits already present
                last4 = BerTlvParser.extractLast4DigitsOnly(selectNodes)
                val pdolSpec = BerTlvParser.findFirstByTag(selectNodes, "9F38")?.copyOf()
                BerTlvParser.wipeNodes(selectNodes)

                if (last4 != null) {
                    pdolSpec?.fill(0)
                    break
                }

                // Step 3: Standard GET PROCESSING OPTIONS (no cryptogram requested)
                val pdolData = BerTlvParser.buildMinimalPdolData(pdolSpec)
                pdolSpec?.fill(0)
                val gpoApdu = buildGpoApdu(pdolData)
                val gpoResp = transceiveAndCheckOk(isoDep, gpoApdu)
                if (gpoResp != null) {
                    val gpoNodes = BerTlvParser.parse(gpoResp)
                    gpoResp.fill(0)

                    last4 = BerTlvParser.extractLast4DigitsOnly(gpoNodes)
                    val aflBytes = extractAflBytes(gpoNodes)
                    BerTlvParser.wipeNodes(gpoNodes)

                    if (last4 != null) {
                        aflBytes?.fill(0)
                        break
                    }

                    // Step 4: Standard READ RECORD on public AFL records (capped)
                    val recordsToRead = if (aflBytes != null) {
                        val parsed = BerTlvParser.parseAflEntries(aflBytes)
                        aflBytes.fill(0)
                        parsed
                    } else {
                        // Gentle fallback: SFI 1 records 1..2, SFI 2 records 1..2
                        listOf(Triple(1, 1, 2), Triple(2, 1, 2))
                    }

                    var readCount = 0
                    outer@ for ((sfi, firstRec, lastRec) in recordsToRead) {
                        for (rec in firstRec..lastRec) {
                            if (readCount >= MAX_RECORDS_TO_READ) break@outer
                            readCount++
                            val readApdu = buildReadRecordApdu(sfi, rec)
                            val recResp = transceiveAndCheckOk(isoDep, readApdu) ?: continue
                            val recNodes = BerTlvParser.parse(recResp)
                            recResp.fill(0)
                            val found = BerTlvParser.extractLast4DigitsOnly(recNodes)
                            BerTlvParser.wipeNodes(recNodes)
                            if (found != null) {
                                last4 = found
                                break@outer
                            }
                        }
                    }

                    if (last4 != null) break
                }
            }

            val metadata = CardPublicMetadata(
                last4Digits = last4,
                applicationLabel = appLabel,
                nfcTechnology = "IsoDep (EMV)",
                isNoCardDemo = false
            )

            return if (metadata.hasVerifiedLast4) {
                NfcReadResult.CardRead(metadata)
            } else {
                NfcReadResult.NumberUnavailable(metadata)
            }
        } catch (_: TagLostException) {
            return NfcReadResult.ReadInterrupted
        } catch (_: IOException) {
            return NfcReadResult.ReadInterrupted
        } catch (_: SecurityException) {
            return NfcReadResult.ReadInterrupted
        } finally {
            try {
                if (isoDep.isConnected) {
                    isoDep.close()
                }
            } catch (_: IOException) {
                // Ignore close error
            }
        }
    }

    private fun extractAflBytes(gpoNodes: List<BerTlvParser.TlvNode>): ByteArray? {
        // Format 2 (Tag 77) contains Tag 94 directly
        BerTlvParser.findFirstByTag(gpoNodes, "94")?.let { return it.copyOf() }

        // Format 1 (Tag 80): 2-byte AIP followed by AFL bytes
        val format1 = BerTlvParser.findFirstByTag(gpoNodes, "80")
        if (format1 != null && format1.size > 2) {
            return format1.copyOfRange(2, format1.size)
        }
        return null
    }

    private fun transceiveAndCheckOk(isoDep: IsoDep, command: ByteArray): ByteArray? {
        val raw = isoDep.transceive(command)
        if (raw.size < 2) {
            raw.fill(0)
            return null
        }
        val sw1 = raw[raw.size - 2].toInt() and 0xFF
        val sw2 = raw[raw.size - 1].toInt() and 0xFF
        if (sw1 == 0x90 && sw2 == 0x00) {
            val payload = raw.copyOfRange(0, raw.size - 2)
            raw.fill(0)
            return payload
        }
        raw.fill(0)
        return null
    }

    private fun buildSelectAidApdu(aid: ByteArray): ByteArray {
        val apdu = ByteArray(6 + aid.size)
        apdu[0] = 0x00
        apdu[1] = 0xA4.toByte()
        apdu[2] = 0x04
        apdu[3] = 0x00
        apdu[4] = aid.size.toByte()
        aid.copyInto(apdu, destinationOffset = 5)
        apdu[apdu.size - 1] = 0x00
        return apdu
    }

    private fun buildGpoApdu(pdolData: ByteArray): ByteArray {
        val lc = pdolData.size + 2
        val apdu = ByteArray(5 + lc + 1)
        apdu[0] = 0x80.toByte()
        apdu[1] = 0xA8.toByte()
        apdu[2] = 0x00
        apdu[3] = 0x00
        apdu[4] = lc.toByte()
        apdu[5] = 0x83.toByte()
        apdu[6] = pdolData.size.toByte()
        pdolData.copyInto(apdu, destinationOffset = 7)
        apdu[apdu.size - 1] = 0x00
        return apdu
    }

    private fun buildReadRecordApdu(sfi: Int, recordNumber: Int): ByteArray {
        val p2 = ((sfi shl 3) or 0x04).toByte()
        return byteArrayOf(
            0x00,
            0xB2.toByte(),
            recordNumber.toByte(),
            p2,
            0x00
        )
    }
}
