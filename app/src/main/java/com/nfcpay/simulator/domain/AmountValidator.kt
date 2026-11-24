package com.nfcpay.simulator.domain

import com.nfcpay.simulator.domain.model.AmountInputState
import com.nfcpay.simulator.domain.model.AmountValidationError

object AmountValidator {

    const val MAX_INTEGER_DIGITS = 9
    const val MAX_DECIMAL_DIGITS = 2
    const val MAX_AMOUNT_KOPECKS = 999_999_999_99L

    fun evaluate(rawInput: String): AmountInputState {
        val normalized = rawInput.trim().replace('.', ',')
        if (normalized.isEmpty()) {
            return AmountInputState(
                rawInput = "",
                amountKopecks = 0L,
                isValid = false,
                validationError = AmountValidationError.EMPTY
            )
        }

        if (normalized.startsWith("-")) {
            return AmountInputState(
                rawInput = normalized,
                amountKopecks = 0L,
                isValid = false,
                validationError = AmountValidationError.ZERO_OR_NEGATIVE
            )
        }

        val parts = normalized.split(',')
        if (parts.size > 2) {
            return AmountInputState(
                rawInput = normalized,
                amountKopecks = 0L,
                isValid = false,
                validationError = AmountValidationError.INVALID_FORMAT
            )
        }

        val intPart = parts[0]
        val decPart = if (parts.size == 2) parts[1] else ""

        if (intPart.isEmpty() || !intPart.all { it.isDigit() } || !decPart.all { it.isDigit() }) {
            return AmountInputState(
                rawInput = normalized,
                amountKopecks = 0L,
                isValid = false,
                validationError = AmountValidationError.INVALID_FORMAT
            )
        }

        if (decPart.length > MAX_DECIMAL_DIGITS) {
            return AmountInputState(
                rawInput = normalized,
                amountKopecks = 0L,
                isValid = false,
                validationError = AmountValidationError.TOO_MANY_DECIMALS
            )
        }

        if (intPart.length > MAX_INTEGER_DIGITS) {
            return AmountInputState(
                rawInput = normalized,
                amountKopecks = 0L,
                isValid = false,
                validationError = AmountValidationError.TOO_LARGE
            )
        }

        val rubles = intPart.toLongOrNull() ?: return AmountInputState(
            rawInput = normalized,
            amountKopecks = 0L,
            isValid = false,
            validationError = AmountValidationError.TOO_LARGE
        )

        val kopecksPart = when (decPart.length) {
            0 -> 0L
            1 -> decPart.toLong() * 10L
            else -> decPart.toLong()
        }

        val totalKopecks = rubles * 100L + kopecksPart
        if (totalKopecks <= 0L) {
            return AmountInputState(
                rawInput = normalized,
                amountKopecks = 0L,
                isValid = false,
                validationError = AmountValidationError.ZERO_OR_NEGATIVE
            )
        }

        if (totalKopecks > MAX_AMOUNT_KOPECKS) {
            return AmountInputState(
                rawInput = normalized,
                amountKopecks = 0L,
                isValid = false,
                validationError = AmountValidationError.TOO_LARGE
            )
        }

        return AmountInputState(
            rawInput = normalized,
            amountKopecks = totalKopecks,
            isValid = true,
            validationError = null
        )
    }

    fun appendDigit(current: AmountInputState, digit: Char): AmountInputState {
        if (!digit.isDigit()) return current
        val raw = current.rawInput
        val commaIndex = raw.indexOf(',')

        val updated = if (commaIndex >= 0) {
            val decimals = raw.length - commaIndex - 1
            if (decimals >= MAX_DECIMAL_DIGITS) return current
            raw + digit
        } else {
            when {
                raw == "0" -> digit.toString()
                raw.length >= MAX_INTEGER_DIGITS -> return current
                else -> raw + digit
            }
        }
        return evaluate(updated)
    }

    fun appendDecimalSeparator(current: AmountInputState): AmountInputState {
        val raw = current.rawInput
        if (raw.contains(',')) return current
        val updated = if (raw.isEmpty()) "0," else "$raw,"
        return evaluate(updated)
    }

    fun backspace(current: AmountInputState): AmountInputState {
        if (current.rawInput.isEmpty()) return current
        return evaluate(current.rawInput.dropLast(1))
    }

    fun clear(): AmountInputState = evaluate("")

    fun formatRubles(amountKopecks: Long, alwaysShowKopecks: Boolean = false): String {
        val safeKopecks = amountKopecks.coerceAtLeast(0L)
        val rubles = safeKopecks / 100L
        val kopecks = safeKopecks % 100L
        val groupedRubles = groupThousands(rubles.toString())
        return if (alwaysShowKopecks || kopecks != 0L) {
            val kopecksStr = kopecks.toString().padStart(2, '0')
            "$groupedRubles,$kopecksStr\u00A0₽"
        } else {
            "$groupedRubles\u00A0₽"
        }
    }

    fun formatLiveInput(rawInput: String): String {
        if (rawInput.isEmpty()) return "0"
        val commaIndex = rawInput.indexOf(',')
        return if (commaIndex >= 0) {
            val intPart = rawInput.substring(0, commaIndex)
            val decPart = rawInput.substring(commaIndex + 1)
            "${groupThousands(intPart.ifEmpty { "0" })},$decPart"
        } else {
            groupThousands(rawInput)
        }
    }

    private fun groupThousands(digits: String): String {
        val sb = StringBuilder()
        val len = digits.length
        for (i in 0 until len) {
            sb.append(digits[i])
            val remaining = len - i - 1
            if (remaining > 0 && remaining % 3 == 0) {
                sb.append('\u00A0')
            }
        }
        return sb.toString()
    }
}
