package com.nfcpay.simulator

import com.nfcpay.simulator.domain.AmountValidator
import com.nfcpay.simulator.domain.model.AmountValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountValidatorTest {

    @Test
    fun `empty input is invalid`() {
        val state = AmountValidator.evaluate("")
        assertFalse(state.isValid)
        assertEquals(0L, state.amountKopecks)
        assertEquals(AmountValidationError.EMPTY, state.validationError)
    }

    @Test
    fun `zero values are rejected`() {
        listOf("0", "0,", "0,0", "0,00", "0.00").forEach { raw ->
            val state = AmountValidator.evaluate(raw)
            assertFalse("Expected $raw to be invalid", state.isValid)
            assertEquals(AmountValidationError.ZERO_OR_NEGATIVE, state.validationError)
        }
    }

    @Test
    fun `negative values are rejected`() {
        listOf("-1", "-10,50", "-0,01").forEach { raw ->
            val state = AmountValidator.evaluate(raw)
            assertFalse("Expected $raw to be invalid", state.isValid)
            assertEquals(AmountValidationError.ZERO_OR_NEGATIVE, state.validationError)
        }
    }

    @Test
    fun `more than two decimal digits are rejected`() {
        val state = AmountValidator.evaluate("12,345")
        assertFalse(state.isValid)
        assertEquals(AmountValidationError.TOO_MANY_DECIMALS, state.validationError)
    }

    @Test
    fun `valid integer and fractional amounts parse to exact kopecks`() {
        val oneKopeck = AmountValidator.evaluate("0,01")
        assertTrue(oneKopeck.isValid)
        assertEquals(1L, oneKopeck.amountKopecks)
        assertNull(oneKopeck.validationError)

        val singleDecimal = AmountValidator.evaluate("150,5")
        assertTrue(singleDecimal.isValid)
        assertEquals(15050L, singleDecimal.amountKopecks)

        val fullDecimal = AmountValidator.evaluate("1250,75")
        assertTrue(fullDecimal.isValid)
        assertEquals(125075L, fullDecimal.amountKopecks)

        val integer = AmountValidator.evaluate("500")
        assertTrue(integer.isValid)
        assertEquals(50000L, integer.amountKopecks)
    }

    @Test
    fun `keypad enforces max two decimal digits and handles leading zeros`() {
        var state = AmountValidator.clear()
        state = AmountValidator.appendDigit(state, '0')
        assertEquals("0", state.rawInput)
        assertFalse(state.isValid)

        // Typing 5 after leading 0 replaces 0
        state = AmountValidator.appendDigit(state, '5')
        assertEquals("5", state.rawInput)
        assertTrue(state.isValid)

        state = AmountValidator.appendDecimalSeparator(state)
        assertEquals("5,", state.rawInput)

        // Second decimal separator is ignored
        state = AmountValidator.appendDecimalSeparator(state)
        assertEquals("5,", state.rawInput)

        state = AmountValidator.appendDigit(state, '2')
        state = AmountValidator.appendDigit(state, '9')
        // Third decimal digit is ignored
        state = AmountValidator.appendDigit(state, '7')
        assertEquals("5,29", state.rawInput)
        assertEquals(529L, state.amountKopecks)

        state = AmountValidator.backspace(state)
        assertEquals("5,2", state.rawInput)
        assertEquals(520L, state.amountKopecks)
    }

    @Test
    fun `decimal separator on empty input prefixes zero`() {
        var state = AmountValidator.clear()
        state = AmountValidator.appendDecimalSeparator(state)
        assertEquals("0,", state.rawInput)
        assertFalse(state.isValid)

        state = AmountValidator.appendDigit(state, '5')
        assertEquals("0,5", state.rawInput)
        assertTrue(state.isValid)
        assertEquals(50L, state.amountKopecks)
    }

    @Test
    fun `ruble formatting groups thousands cleanly`() {
        assertEquals("500\u00A0₽", AmountValidator.formatRubles(50000L))
        assertEquals("1\u00A0250,50\u00A0₽", AmountValidator.formatRubles(125050L))
        assertEquals("1\u00A0000\u00A0000,05\u00A0₽", AmountValidator.formatRubles(100_000_005L))
    }
}
