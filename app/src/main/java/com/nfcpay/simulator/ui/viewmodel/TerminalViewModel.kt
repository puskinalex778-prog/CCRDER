package com.nfcpay.simulator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nfcpay.simulator.data.HistoryRepository
import com.nfcpay.simulator.data.SettingsRepository
import com.nfcpay.simulator.domain.AmountValidator
import com.nfcpay.simulator.domain.model.AppSettings
import com.nfcpay.simulator.domain.model.CardPublicMetadata
import com.nfcpay.simulator.domain.model.DemoTransaction
import com.nfcpay.simulator.domain.model.NfcHardwareStatus
import com.nfcpay.simulator.domain.model.NfcReadResult
import com.nfcpay.simulator.ui.state.FeedbackEvent
import com.nfcpay.simulator.ui.state.TerminalError
import com.nfcpay.simulator.ui.state.TerminalUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class TerminalViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val processingDurationMs: Long = DEFAULT_PROCESSING_DURATION_MS,
    private val waitTimeoutSeconds: Int = TerminalUiState.DEFAULT_WAIT_SECONDS
) : ViewModel() {

    private val _uiState = MutableStateFlow<TerminalUiState>(TerminalUiState.Home)
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private val _nfcStatus = MutableStateFlow(NfcHardwareStatus.NOT_SUPPORTED)
    val nfcStatus: StateFlow<NfcHardwareStatus> = _nfcStatus.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
    val history: StateFlow<List<DemoTransaction>> = historyRepository.history

    private val _feedbackEvents = MutableSharedFlow<FeedbackEvent>(extraBufferCapacity = 8)
    val feedbackEvents: SharedFlow<FeedbackEvent> = _feedbackEvents.asSharedFlow()

    private var countdownJob: Job? = null
    private var processingJob: Job? = null

    fun updateNfcStatus(status: NfcHardwareStatus) {
        _nfcStatus.value = status
        val current = _uiState.value
        if (current is TerminalUiState.WaitingForCard && status != NfcHardwareStatus.ENABLED) {
            cancelCountdown()
            val error = if (status == NfcHardwareStatus.NOT_SUPPORTED) {
                TerminalError.NFC_NOT_SUPPORTED
            } else {
                TerminalError.NFC_DISABLED
            }
            _uiState.value = TerminalUiState.Error(error, current.amountKopecks)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        settingsRepository.setSoundEnabled(enabled)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        settingsRepository.setVibrationEnabled(enabled)
    }

    fun onReceivePaymentClick() {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.AmountEntry(
            input = AmountValidator.clear(),
            isNoCardDemo = false
        )
    }

    fun onDemoWithoutNfcClick() {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.AmountEntry(
            input = AmountValidator.clear(),
            isNoCardDemo = true
        )
    }

    fun onOpenHistoryClick() {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.History
    }

    fun onClearHistoryConfirm() {
        historyRepository.clearHistory()
    }

    fun onDigitPressed(digit: Char) {
        val current = _uiState.value as? TerminalUiState.AmountEntry ?: return
        val updated = AmountValidator.appendDigit(current.input, digit)
        if (updated.rawInput != current.input.rawInput) {
            _feedbackEvents.tryEmit(FeedbackEvent.KEY_TAP)
            _uiState.value = current.copy(input = updated)
        }
    }

    fun onDecimalPressed() {
        val current = _uiState.value as? TerminalUiState.AmountEntry ?: return
        val updated = AmountValidator.appendDecimalSeparator(current.input)
        if (updated.rawInput != current.input.rawInput) {
            _feedbackEvents.tryEmit(FeedbackEvent.KEY_TAP)
            _uiState.value = current.copy(input = updated)
        }
    }

    fun onBackspacePressed() {
        val current = _uiState.value as? TerminalUiState.AmountEntry ?: return
        val updated = AmountValidator.backspace(current.input)
        if (updated.rawInput != current.input.rawInput) {
            _feedbackEvents.tryEmit(FeedbackEvent.KEY_TAP)
            _uiState.value = current.copy(input = updated)
        }
    }

    fun onClearAmountPressed() {
        val current = _uiState.value as? TerminalUiState.AmountEntry ?: return
        if (current.input.rawInput.isNotEmpty()) {
            _feedbackEvents.tryEmit(FeedbackEvent.KEY_TAP)
            _uiState.value = current.copy(input = AmountValidator.clear())
        }
    }

    fun onAmountContinueClick() {
        val current = _uiState.value as? TerminalUiState.AmountEntry ?: return
        if (!current.input.isValid) return

        val amountKopecks = current.input.amountKopecks
        if (current.isNoCardDemo) {
            startProcessing(amountKopecks, CardPublicMetadata.noCardDemo())
        } else {
            // Cardholder consent MUST happen before NFC Reader Mode is ever activated
            _uiState.value = TerminalUiState.CardholderConsent(amountKopecks = amountKopecks)
        }
    }

    fun onConsentConfirmed() {
        val current = _uiState.value as? TerminalUiState.CardholderConsent ?: return
        beginNfcWaitingIfAvailable(current.amountKopecks)
    }

    fun onConsentCancelled() {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.Home
    }

    fun onNfcTagResult(result: NfcReadResult) {
        // Strictly ignore NFC events unless we are actively on WaitingForCard
        val current = _uiState.value as? TerminalUiState.WaitingForCard ?: return
        cancelCountdown()

        when (result) {
            is NfcReadResult.CardRead -> {
                _feedbackEvents.tryEmit(FeedbackEvent.CARD_DETECTED)
                startProcessing(current.amountKopecks, result.metadata)
            }
            is NfcReadResult.NumberUnavailable -> {
                _feedbackEvents.tryEmit(FeedbackEvent.CARD_DETECTED)
                _uiState.value = TerminalUiState.NumberUnavailablePrompt(
                    amountKopecks = current.amountKopecks,
                    metadata = result.metadata.copy(last4Digits = null)
                )
            }
            is NfcReadResult.UnsupportedCard -> {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.UNSUPPORTED_CARD,
                    amountKopecks = current.amountKopecks
                )
            }
            is NfcReadResult.ReadInterrupted -> {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.READ_INTERRUPTED,
                    amountKopecks = current.amountKopecks
                )
            }
        }
    }

    fun onContinueWithoutCardNumber() {
        val current = _uiState.value as? TerminalUiState.NumberUnavailablePrompt ?: return
        startProcessing(
            amountKopecks = current.amountKopecks,
            metadata = current.metadata.copy(last4Digits = null, isNoCardDemo = false)
        )
    }

    fun onRetryWaiting() {
        val amountKopecks = when (val current = _uiState.value) {
            is TerminalUiState.NumberUnavailablePrompt -> current.amountKopecks
            is TerminalUiState.Error -> current.amountKopecks
            else -> null
        } ?: return

        beginNfcWaitingIfAvailable(amountKopecks)
    }

    fun onSwitchToNoCardDemoFromError() {
        val amountKopecks = (_uiState.value as? TerminalUiState.Error)?.amountKopecks
        if (amountKopecks != null && amountKopecks > 0L) {
            startProcessing(amountKopecks, CardPublicMetadata.noCardDemo())
        } else {
            onDemoWithoutNfcClick()
        }
    }

    fun onCancelWaiting() {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.Home
    }

    fun onAppBackgrounded() {
        val current = _uiState.value
        if (current is TerminalUiState.WaitingForCard) {
            cancelCountdown()
            _uiState.value = TerminalUiState.Error(
                error = TerminalError.APP_BACKGROUNDED,
                amountKopecks = current.amountKopecks
            )
        }
    }

    fun onRepeatFromResult() {
        val current = _uiState.value as? TerminalUiState.Result ?: return
        cancelActiveJobs()
        _uiState.value = TerminalUiState.AmountEntry(
            input = AmountValidator.clear(),
            isNoCardDemo = current.metadata.isNoCardDemo
        )
    }

    fun onBackPressed(): Boolean {
        return when (val current = _uiState.value) {
            is TerminalUiState.Home -> false
            is TerminalUiState.CardholderConsent -> {
                _uiState.value = TerminalUiState.AmountEntry(
                    input = AmountValidator.evaluate(formatKopecksAsEditable(current.amountKopecks)),
                    isNoCardDemo = false
                )
                true
            }
            is TerminalUiState.Processing -> {
                // Keep decorative processing uninterrupted or allow cancelling safely
                true
            }
            else -> {
                onBackToHome()
                true
            }
        }
    }

    fun onBackToHome() {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.Home
    }

    private fun beginNfcWaitingIfAvailable(amountKopecks: Long) {
        when (_nfcStatus.value) {
            NfcHardwareStatus.NOT_SUPPORTED -> {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.NFC_NOT_SUPPORTED,
                    amountKopecks = amountKopecks
                )
            }
            NfcHardwareStatus.DISABLED -> {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.NFC_DISABLED,
                    amountKopecks = amountKopecks
                )
            }
            NfcHardwareStatus.ENABLED -> {
                startWaitingCountdown(amountKopecks)
            }
        }
    }

    private fun startWaitingCountdown(amountKopecks: Long) {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.WaitingForCard(
            amountKopecks = amountKopecks,
            remainingSeconds = waitTimeoutSeconds,
            totalSeconds = waitTimeoutSeconds
        )

        countdownJob = viewModelScope.launch {
            var remaining = waitTimeoutSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                val active = _uiState.value
                if (active !is TerminalUiState.WaitingForCard) return@launch
                if (remaining > 0) {
                    _uiState.value = active.copy(remainingSeconds = remaining)
                }
            }
            val active = _uiState.value
            if (active is TerminalUiState.WaitingForCard) {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.TIMEOUT,
                    amountKopecks = active.amountKopecks
                )
            }
        }
    }

    private fun startProcessing(amountKopecks: Long, metadata: CardPublicMetadata) {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.Processing(
            amountKopecks = amountKopecks,
            metadata = metadata
        )

        processingJob = viewModelScope.launch {
            delay(processingDurationMs)
            val timestamp = clock()
            historyRepository.addTransaction(
                DemoTransaction(
                    id = UUID.randomUUID().toString(),
                    amountKopecks = amountKopecks,
                    timestampMillis = timestamp,
                    status = DemoTransaction.STATUS_COMPLETED,
                    openCardInfo = metadata.toOpenHistoryDescription(),
                    isNoCardDemo = metadata.isNoCardDemo
                )
            )
            _uiState.value = TerminalUiState.Result(
                amountKopecks = amountKopecks,
                metadata = metadata,
                timestampMillis = timestamp
            )
            _feedbackEvents.tryEmit(FeedbackEvent.SUCCESS)
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    private fun cancelActiveJobs() {
        countdownJob?.cancel()
        countdownJob = null
        processingJob?.cancel()
        processingJob = null
    }

    private fun formatKopecksAsEditable(amountKopecks: Long): String {
        val rubles = amountKopecks / 100L
        val kopecks = amountKopecks % 100L
        return if (kopecks == 0L) {
            rubles.toString()
        } else {
            "$rubles,${kopecks.toString().padStart(2, '0')}"
        }
    }

    override fun onCleared() {
        cancelActiveJobs()
        super.onCleared()
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val historyRepository: HistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TerminalViewModel(
                settingsRepository = settingsRepository,
                historyRepository = historyRepository
            ) as T
        }
    }

    companion object {
        const val DEFAULT_PROCESSING_DURATION_MS = 1600L
    }
}
