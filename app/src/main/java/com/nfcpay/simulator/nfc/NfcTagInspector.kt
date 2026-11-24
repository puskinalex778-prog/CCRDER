package com.nfcpay.simulator.nfc

import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import com.nfcpay.simulator.domain.model.NdefRecordInfo
import com.nfcpay.simulator.domain.model.NfcScanReadResult
import com.nfcpay.simulator.domain.model.NfcScanRecord
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID

/**
 * Read-only NFC Tag & Public Metadata Inspector.
 *
 * Reads ONLY open, non-sensitive information:
 * - Tag hardware UID & supported NFC technologies
 * - Public NDEF records (Text, URI, MIME)
 * - Basic RF parameters (ATQA, SAK, max transceive size, NDEF capacity)
 * - Public EMV directory metadata (Application Label & AIDs via SELECT PPSE/AID only)
 *
 * Strictly NEVER reads PAN, Track 2, expiry dates, CVV, or generates cryptograms.
 */
class NfcTagInspector(
    private val clock: () -> Long = System::currentTimeMillis
) {

    companion object {
        private const val ISO_DEP_TIMEOUT_MS = 2000

        private val SELECT_PPSE_APDU = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x0E,
            0x32, 0x50, 0x41, 0x59, 0x2E, 0x53, 0x59,
            0x53, 0x2E, 0x44, 0x44, 0x46, 0x30, 0x31,
            0x00
        )
    }

    fun inspectTag(tag: Tag): NfcScanReadResult {
        val techList = tag.techList ?: emptyArray()
        if (techList.isEmpty()) {
            return NfcScanReadResult.UnsupportedTag
        }

        val simpleTechs = techList.map { it.substringAfterLast('.') }.distinct()
        val uidHex = NdefPayloadDecoder.formatUidColonHex(tag.id)
        val details = mutableListOf<String>()
        val ndefItems = mutableListOf<NdefRecordInfo>()
        var appLabel: String? = null
        val publicAids = mutableListOf<String>()

        try {
            // 1. Read public NDEF message if available
            Ndef.get(tag)?.let { ndef ->
                try {
                    val cached = ndef.cachedNdefMessage ?: run {
                        ndef.connect()
                        ndef.ndefMessage
                    }
                    details.add("NDEF тип: ${ndef.type.ifEmpty { "стандартный" }}")
                    if (ndef.maxSize > 0) {
                        details.add("Объём NDEF: ${ndef.maxSize} байт (${if (ndef.isWritable) "перезаписываемая" else "только чтение"})")
                    }
                    cached?.records?.forEach { rec ->
                        decodeNdefRecord(rec)?.let { ndefItems.add(it) }
                    }
                } finally {
                    safeClose { if (ndef.isConnected) ndef.close() }
                }
            }

            // 2. Collect open low-level RF parameters (no private data)
            NfcA.get(tag)?.let { nfcA ->
                val atqaBytes = nfcA.atqa
                val atqa = if (atqaBytes != null && atqaBytes.isNotEmpty()) {
                    BerTlvParser.bytesToHex(atqaBytes)
                } else {
                    "—"
                }
                val sak = String.format(Locale.US, "0x%02X", nfcA.sak.toInt() and 0xFF)
                details.add("NfcA: ATQA $atqa · SAK $sak")
            }

            NfcB.get(tag)?.let { nfcB ->
                val proto = nfcB.protocolInfo
                if (proto != null && proto.isNotEmpty()) {
                    details.add("NfcB: протокол ${BerTlvParser.bytesToHex(proto)}")
                }
            }

            NfcF.get(tag)?.let { nfcF ->
                val sysCode = nfcF.systemCode
                if (sysCode != null && sysCode.isNotEmpty()) {
                    details.add("NfcF: код системы ${BerTlvParser.bytesToHex(sysCode)}")
                }
            }

            NfcV.get(tag)?.let { nfcV ->
                details.add("NfcV: DSF ID 0x${String.format(Locale.US, "%02X", nfcV.dsfId.toInt() and 0xFF)}")
            }

            // 3. For IsoDep cards: read ONLY public PPSE directory (Application Label & AIDs)
            IsoDep.get(tag)?.let { isoDep ->
                try {
                    isoDep.connect()
                    isoDep.timeout = ISO_DEP_TIMEOUT_MS
                    details.add("IsoDep: макс. пакет ${isoDep.maxTransceiveLength} байт")

                    val ppseRaw = isoDep.transceive(SELECT_PPSE_APDU)
                    val ppsePayload = extractOkPayload(ppseRaw)
                    ppseRaw.fill(0)

                    if (ppsePayload != null) {
                        val nodes = BerTlvParser.parse(ppsePayload)
                        ppsePayload.fill(0)
                        appLabel = BerTlvParser.extractApplicationLabel(nodes)
                        val foundAids = BerTlvParser.findAllByTag(nodes, "4F")
                        for (aid in foundAids) {
                            if (aid.size in 5..16) {
                                publicAids.add(BerTlvParser.bytesToHex(aid))
                            }
                        }

                        // If PPSE didn't include a label, SELECT the first public AID purely for Tag 50 / 9F12 label
                        if (appLabel == null && foundAids.isNotEmpty()) {
                            val firstAid = foundAids.first().copyOf()
                            BerTlvParser.wipeNodes(nodes)
                            val selectAid = buildSelectAidApdu(firstAid)
                            val aidRaw = isoDep.transceive(selectAid)
                            val aidPayload = extractOkPayload(aidRaw)
                            aidRaw.fill(0)
                            if (aidPayload != null) {
                                val aidNodes = BerTlvParser.parse(aidPayload)
                                aidPayload.fill(0)
                                appLabel = BerTlvParser.extractApplicationLabel(aidNodes)
                                BerTlvParser.wipeNodes(aidNodes)
                            }
                        } else {
                            BerTlvParser.wipeNodes(nodes)
                        }
                    }
                } finally {
                    safeClose { if (isoDep.isConnected) isoDep.close() }
                }
            }

            val defaultTitle = buildDefaultTitle(
                appLabel = appLabel,
                ndefRecords = ndefItems,
                simpleTechs = simpleTechs,
                uidHex = uidHex
            )

            return NfcScanReadResult.Scanned(
                NfcScanRecord(
                    id = UUID.randomUUID().toString(),
                    title = defaultTitle,
                    timestampMillis = clock(),
                    uidHex = uidHex,
                    technologies = simpleTechs,
                    applicationLabel = appLabel,
                    publicAids = publicAids.distinct(),
                    ndefRecords = ndefItems,
                    technicalSummary = details.joinToString("\n"),
                    note = ""
                )
            )
        } catch (_: TagLostException) {
            return NfcScanReadResult.ReadInterrupted
        } catch (_: IOException) {
            return NfcScanReadResult.ReadInterrupted
        } catch (_: SecurityException) {
            return NfcScanReadResult.ReadInterrupted
        }
    }

    private fun decodeNdefRecord(record: NdefRecord): NdefRecordInfo? {
        return when (record.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> when {
                record.type.contentEquals(NdefRecord.RTD_TEXT) ->
                    NdefPayloadDecoder.decodeWellKnownText(record.payload)
                record.type.contentEquals(NdefRecord.RTD_URI) ->
                    NdefPayloadDecoder.decodeWellKnownUri(record.payload)
                else -> NdefRecordInfo(
                    recordType = "NDEF (${String(record.type, StandardCharsets.US_ASCII)})",
                    content = "Данные (${record.payload.size} байт)"
                )
            }
            NdefRecord.TNF_MIME_MEDIA -> {
                val mime = String(record.type, StandardCharsets.US_ASCII).ifEmpty { "application/octet-stream" }
                NdefPayloadDecoder.decodeMimePayload(mime, record.payload)
            }
            NdefRecord.TNF_ABSOLUTE_URI -> {
                val uri = String(record.type, StandardCharsets.UTF_8).trim()
                if (uri.isNotEmpty()) NdefRecordInfo("URI", uri) else null
            }
            NdefRecord.TNF_EXTERNAL_TYPE -> {
                val extType = String(record.type, StandardCharsets.US_ASCII)
                NdefPayloadDecoder.decodeMimePayload("external:$extType", record.payload)
            }
            else -> null
        }
    }

    private fun buildDefaultTitle(
        appLabel: String?,
        ndefRecords: List<NdefRecordInfo>,
        simpleTechs: List<String>,
        uidHex: String?
    ): String {
        if (!appLabel.isNullOrBlank()) {
            return "Карта · $appLabel"
        }
        val firstNdef = ndefRecords.firstOrNull()?.content?.take(28)
        if (!firstNdef.isNullOrBlank()) {
            return "Метка · $firstNdef"
        }
        val primaryTech = simpleTechs.firstOrNull() ?: "NFC"
        val shortUid = uidHex?.take(11)
        return if (shortUid != null) {
            "NFC ($primaryTech) · $shortUid"
        } else {
            "NFC-метка ($primaryTech)"
        }
    }

    private fun extractOkPayload(raw: ByteArray): ByteArray? {
        if (raw.size < 2) return null
        val sw1 = raw[raw.size - 2].toInt() and 0xFF
        val sw2 = raw[raw.size - 1].toInt() and 0xFF
        return if (sw1 == 0x90 && sw2 == 0x00) {
            raw.copyOfRange(0, raw.size - 2)
        } else {
            null
        }
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

    private inline fun safeClose(block: () -> Unit) {
        try {
            block()
        } catch (_: IOException) {
            // Ignore close exception
        }
    }
}
