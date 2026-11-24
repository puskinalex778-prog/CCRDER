package com.nfcpay.simulator.ui.state

import com.nfcpay.simulator.domain.AmountValidator
import com.nfcpay.simulator.domain.model.AmountInputState
import com.nfcpay.simulator.domain.model.CardPublicMetadata

sealed interface TerminalUiState {

    data object Home : TerminalUiState

    data class AmountEntry(
        val input: AmountInputState = AmountValidator.clear(),
        val isNoCardDemo: Boolean = false
    ) : TerminalUiState

    data class CardholderConsent(
        val amountKopecks: Long
    ) : TerminalUiState

    data class WaitingForCard(
        val amountKopecks: Long,
        val remainingSeconds: Int = DEFAULT_WAIT_SECONDS,
        val totalSeconds: Int = DEFAULT_WAIT_SECONDS
    ) : TerminalUiState

    data class NumberUnavailablePrompt(
        val amountKopecks: Long,
        val metadata: CardPublicMetadata
    ) : TerminalUiState

    data class Processing(
        val amountKopecks: Long,
        val metadata: CardPublicMetadata
    ) : TerminalUiState

    data class Result(
        val amountKopecks: Long,
        val metadata: CardPublicMetadata,
        val timestampMillis: Long
    ) : TerminalUiState

    data class Error(
        val error: TerminalError,
        val amountKopecks: Long? = null
    ) : TerminalUiState

    data object History : TerminalUiState

    companion object {
        const val DEFAULT_WAIT_SECONDS = 30
    }
}

enum class TerminalError {
    NFC_NOT_SUPPORTED,
    NFC_DISABLED,
    UNSUPPORTED_CARD,
    READ_INTERRUPTED,
    TIMEOUT,
    APP_BACKGROUNDED
}

enum class FeedbackEvent {
    KEY_TAP,
    CARD_DETECTED,
    SUCCESS,
    ERROR
}
