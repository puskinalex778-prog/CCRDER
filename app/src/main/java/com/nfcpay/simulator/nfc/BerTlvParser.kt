package com.nfcpay.simulator.nfc

import java.nio.charset.StandardCharsets

/**
 * Minimal, pure-Kotlin BER-TLV parser for standard read-only EMV public metadata.
 * Contains zero logging and extracts only non-sensitive metadata or the last 4 digits of PAN.
 */
object BerTlvParser {

    data class TlvNode(
        val tagHex: String,
        val value: ByteArray,
        val children: List<TlvNode>
    )

    fun parse(data: ByteArray, depth: Int = 0): List<TlvNode> {
        if (depth > 8 || data.isEmpty()) return emptyList()
        val nodes = mutableListOf<TlvNode>()
        var offset = 0

        while (offset < data.size) {
            val firstByte = data[offset].toInt() and 0xFF
            if (firstByte == 0x00 || firstByte == 0xFF) {
                offset++
                continue
            }

            val tagStart = offset
            val isConstructed = (firstByte and 0x20) != 0
            offset++

            // Multi-byte tag if low 5 bits are all 1s
            if ((firstByte and 0x1F) == 0x1F) {
                while (offset < data.size) {
                    val nextByte = data[offset].toInt() and 0xFF
                    offset++
                    if ((nextByte and 0x80) == 0) break
                }
            }
            if (offset >= data.size) break

            val tagHex = bytesToHex(data, tagStart, offset - tagStart)

            // Parse length
            val lenByte = data[offset].toInt() and 0xFF
            offset++
            val length: Int = if ((lenByte and 0x80) == 0) {
                lenByte
            } else {
                val numLenBytes = lenByte and 0x7F
                if (numLenBytes == 0 || numLenBytes > 3 || offset + numLenBytes > data.size) break
                var acc = 0
                repeat(numLenBytes) {
                    acc = (acc shl 8) or (data[offset].toInt() and 0xFF)
                    offset++
                }
                acc
            }

            if (length < 0 || offset + length > data.size) break

            val value = data.copyOfRange(offset, offset + length)
            offset += length

            val children = if (isConstructed && value.isNotEmpty()) {
                parse(value, depth + 1)
            } else {
                emptyList()
            }
            nodes.add(TlvNode(tagHex = tagHex, value = value, children = children))
        }
        return nodes
    }

    fun findAllByTag(nodes: List<TlvNode>, tagHex: String): List<ByteArray> {
        val target = tagHex.uppercase()
        val result = mutableListOf<ByteArray>()
        fun walk(list: List<TlvNode>) {
            for (node in list) {
                if (node.tagHex == target) {
                    result.add(node.value)
                }
                if (node.children.isNotEmpty()) {
                    walk(node.children)
                }
            }
        }
        walk(nodes)
        return result
    }

    fun findFirstByTag(nodes: List<TlvNode>, tagHex: String): ByteArray? =
        findAllByTag(nodes, tagHex).firstOrNull()

    /**
     * Extracts ONLY the last 4 decimal digits from EMV Tag 5A (PAN) or Tag 57 (Track 2 Equivalent).
     * Never returns the full card number, expiry date, or service code.
     */
    fun extractLast4DigitsOnly(nodes: List<TlvNode>): String? {
        findFirstByTag(nodes, "5A")?.let { panBytes ->
            val last4 = extractLast4FromBcdDigits(panBytes, stopAtSeparator = false)
            panBytes.fill(0)
            if (last4 != null) return last4
        }

        findFirstByTag(nodes, "57")?.let { track2Bytes ->
            val last4 = extractLast4FromBcdDigits(track2Bytes, stopAtSeparator = true)
            track2Bytes.fill(0)
            if (last4 != null) return last4
        }

        return null
    }

    /**
     * Parses BCD nibbles from Tag 5A or Tag 57 (stopping before 'D' separator in Track 2)
     * and keeps only the trailing 4 decimal digits.
     */
    fun extractLast4FromBcdDigits(bytes: ByteArray, stopAtSeparator: Boolean): String? {
        val ring = CharArray(4)
        var count = 0
        var totalDigits = 0

        loop@ for (b in bytes) {
            val high = (b.toInt() ushr 4) and 0x0F
            val low = b.toInt() and 0x0F

            for (nibble in intArrayOf(high, low)) {
                if (nibble in 0..9) {
                    ring[count % 4] = ('0'.code + nibble).toChar()
                    count++
                    totalDigits++
                } else if (nibble == 0x0D && stopAtSeparator) {
                    break@loop
                } else if (nibble == 0x0F) {
                    break@loop
                } else {
                    break@loop
                }
            }
        }

        // Standard card PAN has at least 12 digits (typically 16-19)
        if (totalDigits < 12) {
            ring.fill('\u0000')
            return null
        }

        val ordered = CharArray(4)
        val start = count % 4
        for (i in 0 until 4) {
            ordered[i] = ring[(start + i) % 4]
        }
        ring.fill('\u0000')
        val result = String(ordered)
        ordered.fill('\u0000')
        return result
    }

    /**
     * Extracts clean printable ASCII/UTF-8 public application label (Tag 50 or 9F12).
     */
    fun extractApplicationLabel(nodes: List<TlvNode>): String? {
        val raw = findFirstByTag(nodes, "9F12") ?: findFirstByTag(nodes, "50") ?: return null
        val text = String(raw, StandardCharsets.US_ASCII)
            .filter { it.code in 0x20..0x7E }
            .trim()
        return text.takeIf { it.isNotEmpty() }
    }

    /**
     * Parses PDOL (Tag 9F38) and constructs a minimal read-only PDOL data block
     * with benign terminal values (currency RUB 0643, country RU 0643, zero amount)
     * strictly for standard GET PROCESSING OPTIONS public record discovery.
     */
    fun buildMinimalPdolData(pdolSpec: ByteArray?): ByteArray {
        if (pdolSpec == null || pdolSpec.isEmpty()) return ByteArray(0)
        val out = mutableListOf<Byte>()
        var offset = 0

        while (offset < pdolSpec.size) {
            val tagStart = offset
            val first = pdolSpec[offset].toInt() and 0xFF
            offset++
            if ((first and 0x1F) == 0x1F) {
                while (offset < pdolSpec.size) {
                    val b = pdolSpec[offset].toInt() and 0xFF
                    offset++
                    if ((b and 0x80) == 0) break
                }
            }
            if (offset >= pdolSpec.size) break
            val tagHex = bytesToHex(pdolSpec, tagStart, offset - tagStart)
            val reqLen = pdolSpec[offset].toInt() and 0xFF
            offset++
            if (reqLen <= 0 || reqLen > 32) continue

            val value = ByteArray(reqLen)
            when (tagHex) {
                // Terminal Transaction Qualifiers (TTQ) - contactless EMV read-only capability
                "9F66" -> {
                    if (reqLen >= 1) value[0] = 0x36.toByte()
                    if (reqLen >= 2) value[1] = 0x00.toByte()
                    if (reqLen >= 3) value[2] = 0x40.toByte()
                    if (reqLen >= 4) value[3] = 0x00.toByte()
                }
                // Terminal Country Code / Transaction Currency Code (643 = RUB / RU)
                "9F1A", "5F2A" -> {
                    if (reqLen == 2) {
                        value[0] = 0x06.toByte()
                        value[1] = 0x43.toByte()
                    }
                }
                // Unpredictable Number (benign static non-zero demo bytes)
                "9F37" -> {
                    for (i in value.indices) value[i] = (0x11 + i).toByte()
                }
            }
            out.addAll(value.toList())
        }
        return out.toByteArray()
    }

    /**
     * Parses AFL (Tag 94) into (sfi, startRecord, endRecord) triples for read-only READ RECORD.
     */
    fun parseAflEntries(aflBytes: ByteArray): List<Triple<Int, Int, Int>> {
        val entries = mutableListOf<Triple<Int, Int, Int>>()
        var i = 0
        while (i + 3 < aflBytes.size) {
            val sfi = (aflBytes[i].toInt() and 0xF8) ushr 3
            val firstRec = aflBytes[i + 1].toInt() and 0xFF
            val lastRec = aflBytes[i + 2].toInt() and 0xFF
            if (sfi in 1..30 && firstRec in 1..lastRec) {
                entries.add(Triple(sfi, firstRec, lastRec))
            }
            i += 4
        }
        return entries
    }

    fun wipeNodes(nodes: List<TlvNode>) {
        for (node in nodes) {
            node.value.fill(0)
            if (node.children.isNotEmpty()) {
                wipeNodes(node.children)
            }
        }
    }

    fun bytesToHex(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): String {
        val chars = CharArray(length * 2)
        val hexChars = "0123456789ABCDEF"
        for (i in 0 until length) {
            val v = bytes[offset + i].toInt() and 0xFF
            chars[i * 2] = hexChars[v ushr 4]
            chars[i * 2 + 1] = hexChars[v and 0x0F]
        }
        return String(chars)
    }
}
