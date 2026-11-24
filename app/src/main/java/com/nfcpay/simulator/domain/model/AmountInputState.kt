package com.nfcpay.simulator.domain.model

data class AmountInputState(
    val rawInput: String = "",
    val amountKopecks: Long = 0L,
    val isValid: Boolean = false,
    val validationError: AmountValidationError? = null
)

enum class AmountValidationError {
    EMPTY,
    ZERO_OR_NEGATIVE,
    TOO_MANY_DECIMALS,
    INVALID_FORMAT,
    TOO_LARGE
}
