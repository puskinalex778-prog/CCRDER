package com.nfcpay.simulator

import com.nfcpay.simulator.nfc.BerTlvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BerTlvParserTest {

    @Test
    fun `extracts only last 4 digits from Tag 5A synthetic PAN`() {
        // Synthetic test TLV: Tag 70 -> Tag 5A (8 bytes = 16 synthetic digits: 4000 0012 3456 7890)
        val tlv = byteArrayOf(
            0x70, 0x0A,
            0x5A, 0x08,
            0x40, 0x00, 0x00, 0x12, 0x34, 0x56, 0x78, 0x90.toByte()
        )
        val nodes = BerTlvParser.parse(tlv)
        assertEquals("7890", BerTlvParser.extractLast4DigitsOnly(nodes))
    }

    @Test
    fun `extracts only last 4 digits before separator D from Tag 57 Track 2`() {
        // Synthetic Track 2: 2200 1122 3344 5566 D 2812 101...
        val tlv = byteArrayOf(
            0x70, 0x0D,
            0x57, 0x0B,
            0x22, 0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66,
            0xD2.toByte(), 0x81.toByte(), 0x21
        )
        val nodes = BerTlvParser.parse(tlv)
        assertEquals("5566", BerTlvParser.extractLast4DigitsOnly(nodes))
    }

    @Test
    fun `rejects too short digit sequence`() {
        val shortBytes = byteArrayOf(0x12, 0x34, 0x56)
        assertNull(BerTlvParser.extractLast4FromBcdDigits(shortBytes, stopAtSeparator = false))
    }

    @Test
    fun `extracts public application label from Tag 50`() {
        // Tag 50 = "MIR" (0x4D, 0x49, 0x52)
        val tlv = byteArrayOf(0x50, 0x03, 0x4D, 0x49, 0x52)
        val nodes = BerTlvParser.parse(tlv)
        assertEquals("MIR", BerTlvParser.extractApplicationLabel(nodes))
    }

    @Test
    fun `parses AFL entries cleanly`() {
        // SFI 1 (0x08), rec 1..2; SFI 2 (0x10), rec 1..3
        val afl = byteArrayOf(0x08, 0x01, 0x02, 0x00, 0x10, 0x01, 0x03, 0x01)
        val parsed = BerTlvParser.parseAflEntries(afl)
        assertEquals(listOf(Triple(1, 1, 2), Triple(2, 1, 3)), parsed)
    }
}
