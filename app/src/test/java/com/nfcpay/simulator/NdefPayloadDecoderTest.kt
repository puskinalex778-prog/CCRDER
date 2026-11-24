package com.nfcpay.simulator

import com.nfcpay.simulator.nfc.NdefPayloadDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.StandardCharsets

class NdefPayloadDecoderTest {

    @Test
    fun `decodes UTF-8 NDEF well-known text payload`() {
        val textBytes = "Привет NFC".toByteArray(StandardCharsets.UTF_8)
        val langBytes = "ru".toByteArray(StandardCharsets.US_ASCII)
        val payload = byteArrayOf(langBytes.size.toByte()) + langBytes + textBytes

        val decoded = NdefPayloadDecoder.decodeWellKnownText(payload)
        assertNotNull(decoded)
        assertEquals("Текст (NDEF)", decoded?.recordType)
        assertEquals("Привет NFC", decoded?.content)
    }

    @Test
    fun `decodes NDEF well-known URI with https prefix`() {
        val suffix = "example.com/tag".toByteArray(StandardCharsets.UTF_8)
        val payload = byteArrayOf(0x04) + suffix

        val decoded = NdefPayloadDecoder.decodeWellKnownUri(payload)
        assertNotNull(decoded)
        assertEquals("Ссылка (URI)", decoded?.recordType)
        assertEquals("https://example.com/tag", decoded?.content)
    }

    @Test
    fun `formats UID bytes as colon-separated uppercase hex`() {
        val uid = byteArrayOf(0x04, 0xA1.toByte(), 0x0F, 0xFF.toByte())
        assertEquals("04:A1:0F:FF", NdefPayloadDecoder.formatUidColonHex(uid))
        assertNull(NdefPayloadDecoder.formatUidColonHex(byteArrayOf()))
    }
}
