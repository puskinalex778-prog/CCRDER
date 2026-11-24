package com.nfcpay.simulator.domain.model

sealed interface NfcReadResult {
    data class CardRead(val metadata: CardPublicMetadata) : NfcReadResult
    data class NumberUnavailable(val metadata: CardPublicMetadata) : NfcReadResult
    data object UnsupportedCard : NfcReadResult
    data object ReadInterrupted : NfcReadResult
}
