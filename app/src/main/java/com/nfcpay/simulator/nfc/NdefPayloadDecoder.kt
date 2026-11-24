package com.nfcpay.simulator.nfc

import com.nfcpay.simulator.domain.model.NdefRecordInfo
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Pure-Kotlin decoder for standard NFC Forum NDEF record payloads (Text, URI, MIME).
 */
object NdefPayloadDecoder {

    private val URI_PREFIXES = arrayOf(
        "",
        "http://www.",
        "https://www.",
        "http://",
        "https://",
        "tel:",
        "mailto:",
        "ftp://anonymous:anonymous@",
        "ftp://ftp.",
        "ftps://",
        "sftp://",
        "smb://",
        "nfs://",
        "ftp://",
        "dav://",
        "news:",
        "telnet://",
        "imap:",
        "rtsp://",
        "urn:",
        "pop:",
        "sip:",
        "sips:",
        "tftp:",
        "btspp://",
        "btl2cap://",
        "btgoep://",
        "tcpobex://",
        "irdaobex://",
        "file://",
        "urn:epc:id:",
        "urn:epc:tag:",
        "urn:epc:pat:",
        "urn:epc:raw:",
        "urn:epc:",
        "urn:nfc:"
    )

    fun decodeWellKnownText(payload: ByteArray): NdefRecordInfo? {
        if (payload.isEmpty()) return null
        val status = payload[0].toInt() and 0xFF
        val isUtf16 = (status and 0x80) != 0
        val langLength = status and 0x3F
        val textStart = 1 + langLength
        if (textStart > payload.size) return null

        val charset = if (isUtf16) StandardCharsets.UTF_16 else StandardCharsets.UTF_8
        val text = String(payload, textStart, payload.size - textStart, charset).trim()
        return if (text.isNotEmpty()) {
            NdefRecordInfo(recordType = "Текст (NDEF)", content = text)
        } else {
            null
        }
    }

    fun decodeWellKnownUri(payload: ByteArray): NdefRecordInfo? {
        if (payload.isEmpty()) return null
        val prefixIndex = payload[0].toInt() and 0xFF
        val prefix = URI_PREFIXES.getOrElse(prefixIndex) { "" }
        val suffix = String(payload, 1, payload.size - 1, StandardCharsets.UTF_8).trim()
        val fullUri = (prefix + suffix).trim()
        return if (fullUri.isNotEmpty()) {
            NdefRecordInfo(recordType = "Ссылка (URI)", content = fullUri)
        } else {
            null
        }
    }

    fun decodeMimePayload(mimeType: String, payload: ByteArray): NdefRecordInfo {
        val printable = String(payload, StandardCharsets.UTF_8)
            .filter { it == '\n' || it == '\t' || it.code >= 0x20 }
            .trim()
            .take(512)
        val content = if (printable.isNotEmpty()) {
            printable
        } else {
            "Бинарные данные (${payload.size} байт)"
        }
        return NdefRecordInfo(recordType = "MIME: $mimeType", content = content)
    }

    fun formatUidColonHex(uidBytes: ByteArray?): String? {
        if (uidBytes == null || uidBytes.isEmpty()) return null
        return uidBytes.joinToString(":") { b ->
            String.format(Locale.US, "%02X", b.toInt() and 0xFF)
        }
    }
}
