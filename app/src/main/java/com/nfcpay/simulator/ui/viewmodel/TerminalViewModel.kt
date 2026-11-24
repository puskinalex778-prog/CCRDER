package com.nfcpay.simulator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nfcpay.simulator.data.HistoryRepository
import com.nfcpay.simulator.data.NfcScanRepository
import com.nfcpay.simulator.data.SettingsRepository
import com.nfcpay.simulator.domain.AmountValidator
import com.nfcpay.simulator.domain.model.AppSettings
import com.nfcpay.simulator.domain.model.CardPublicMetadata
import com.nfcpay.simulator.domain.model.DemoTransaction
import com.nfcpay.simulator.domain.model.NfcHardwareStatus
import com.nfcpay.simulator.domain.model.NfcReadResult
import com.nfcpay.simulator.domain.model.NfcScanReadResult
import com.nfcpay.simulator.domain.model.NfcScanRecord
import com.nfcpay.simulator.domain.model.PaymentDirection
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
    private val nfcScanRepository: NfcScanRepository,
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
    val savedScans: StateFlow<List<NfcScanRecord>> = nfcScanRepository.scans

    private val _feedbackEvents = MutableSharedFlow<FeedbackEvent>(extraBufferCapacity = 8)
    val feedbackEvents: SharedFlow<FeedbackEvent> = _feedbackEvents.asSharedFlow()

    private var countdownJob: Job? = null
    private var processingJob: Job? = null

    fun updateNfcStatus(status: NfcHardwareStatus) {
        _nfcStatus.value = status
        when (val current = _uiState.value) {
            is TerminalUiState.WaitingForCard -> {
                if (status != NfcHardwareStatus.ENABLED) {
                    cancelCountdown()
                    val error = if (status == NfcHardwareStatus.NOT_SUPPORTED) {
                        TerminalError.NFC_NOT_SUPPORTED
                    } else {
                        TerminalError.NFC_DISABLED
                    }
                    _uiState.value = TerminalUiState.Error(
                        error = error,
                        amountKopecks = current.amountKopecks,
                        direction = current.direction
                    )
                }
            }
            is TerminalUiState.NfcScannerReading -> {
                if (status != NfcHardwareStatus.ENABLED) {
                    cancelCountdown()
                    val error = if (status == NfcHardwareStatus.NOT_SUPPORTED) {
                        TerminalError.NFC_NOT_SUPPORTED
                    } else {
                        TerminalError.NFC_DISABLED
                    }
                    _uiState.value = TerminalUiState.Error(
                        error = error,
                        isFromScanner = true
                    )
                }
            }
            else -> Unit
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
            isNoCardDemo = false,
            direction = PaymentDirection.RECEIVE
        )
    }

    fun onSendToCardClick() {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.AmountEntry(
            input = AmountValidator.clear(),
            isNoCardDemo = false,
            direction = PaymentDirection.SEND
        )
    }

    fun onDemoWithoutNfcClick(direction: PaymentDirection = PaymentDirection.RECEIVE) {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.AmountEntry(
            input = AmountValidator.clear(),
            isNoCardDemo = true,
            direction = direction
        )
    }

    fun onOpenHistoryClick() {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.History
    }

    fun onClearHistoryConfirm() {
        historyRepository.clearHistory()
    }

    // --- NFC Data Scanner & Saved Tags ---

    fun onOpenNfcScannerClick() {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.NfcScannerHome
    }

    fun onStartNfcScanClick() {
        when (_nfcStatus.value) {
            NfcHardwareStatus.NOT_SUPPORTED -> {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.NFC_NOT_SUPPORTED,
                    isFromScanner = true
                )
            }
            NfcHardwareStatus.DISABLED -> {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.NFC_DISABLED,
                    isFromScanner = true
                )
            }
            NfcHardwareStatus.ENABLED -> {
                startScannerCountdown()
            }
        }
    }

    fun onNfcScanResult(result: NfcScanReadResult) {
        if (_uiState.value !is TerminalUiState.NfcScannerReading) return
        cancelCountdown()

        when (result) {
            is NfcScanReadResult.Scanned -> {
                _feedbackEvents.tryEmit(FeedbackEvent.CARD_DETECTED)
                _uiState.value = TerminalUiState.NfcScanPreview(draft = result.record)
            }
            is NfcScanReadResult.UnsupportedTag -> {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.UNSUPPORTED_CARD,
                    isFromScanner = true
                )
            }
            is NfcScanReadResult.ReadInterrupted -> {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.READ_INTERRUPTED,
                    isFromScanner = true
                )
            }
        }
    }

    fun onSaveScannedTag(title: String, note: String) {
        val current = _uiState.value as? TerminalUiState.NfcScanPreview ?: return
        val cleanTitle = title.trim().ifEmpty { current.draft.title }
        val saved = current.draft.copy(
            title = cleanTitle,
            note = note.trim()
        )
        nfcScanRepository.saveScan(saved)
        _feedbackEvents.tryEmit(FeedbackEvent.SUCCESS)
        _uiState.value = TerminalUiState.NfcScanDetail(record = saved)
    }

    fun onOpenSavedScanDetail(record: NfcScanRecord) {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.NfcScanDetail(record = record)
    }

    fun onDeleteSavedScan(id: String) {
        nfcScanRepository.deleteScan(id)
        if (_uiState.value is TerminalUiState.NfcScanDetail) {
            _uiState.value = TerminalUiState.NfcScannerHome
        }
    }

    fun onClearSavedScansConfirm() {
        nfcScanRepository.clearScans()
    }

    // --- Amount Input & Payment / Reverse Send Flow ---

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
            startProcessing(
                amountKopecks = amountKopecks,
                metadata = CardPublicMetadata.noCardDemo(),
                direction = current.direction
            )
        } else {
            // Cardholder consent MUST happen before NFC Reader Mode is ever activated
            _uiState.value = TerminalUiState.CardholderConsent(
                amountKopecks = amountKopecks,
                direction = current.direction
            )
        }
    }

    fun onConsentConfirmed() {
        val current = _uiState.value as? TerminalUiState.CardholderConsent ?: return
        beginNfcWaitingIfAvailable(current.amountKopecks, current.direction)
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
                startProcessing(current.amountKopecks, result.metadata, current.direction)
            }
            is NfcReadResult.NumberUnavailable -> {
                _feedbackEvents.tryEmit(FeedbackEvent.CARD_DETECTED)
                _uiState.value = TerminalUiState.NumberUnavailablePrompt(
                    amountKopecks = current.amountKopecks,
                    metadata = result.metadata.copy(last4Digits = null),
                    direction = current.direction
                )
            }
            is NfcReadResult.UnsupportedCard -> {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.UNSUPPORTED_CARD,
                    amountKopecks = current.amountKopecks,
                    direction = current.direction
                )
            }
            is NfcReadResult.ReadInterrupted -> {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.READ_INTERRUPTED,
                    amountKopecks = current.amountKopecks,
                    direction = current.direction
                )
            }
        }
    }

    fun onContinueWithoutCardNumber() {
        val current = _uiState.value as? TerminalUiState.NumberUnavailablePrompt ?: return
        startProcessing(
            amountKopecks = current.amountKopecks,
            metadata = current.metadata.copy(last4Digits = null, isNoCardDemo = false),
            direction = current.direction
        )
    }

    fun onRetryWaiting() {
        when (val current = _uiState.value) {
            is TerminalUiState.NumberUnavailablePrompt ->
                beginNfcWaitingIfAvailable(current.amountKopecks, current.direction)
            is TerminalUiState.Error -> {
                if (current.isFromScanner) {
                    onStartNfcScanClick()
                } else if (current.amountKopecks != null) {
                    beginNfcWaitingIfAvailable(current.amountKopecks, current.direction)
                }
            }
            else -> Unit
        }
    }

    fun onSwitchToNoCardDemoFromError() {
        val current = _uiState.value as? TerminalUiState.Error
        val amountKopecks = current?.amountKopecks
        val direction = current?.direction ?: PaymentDirection.RECEIVE
        if (amountKopecks != null && amountKopecks > 0L) {
            startProcessing(amountKopecks, CardPublicMetadata.noCardDemo(), direction)
        } else {
            onDemoWithoutNfcClick(direction)
        }
    }

    fun onCancelWaiting() {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.Home
    }

    fun onAppBackgrounded() {
        when (val current = _uiState.value) {
            is TerminalUiState.WaitingForCard -> {
                cancelCountdown()
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.APP_BACKGROUNDED,
                    amountKopecks = current.amountKopecks,
                    direction = current.direction
                )
            }
            is TerminalUiState.NfcScannerReading -> {
                cancelCountdown()
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.APP_BACKGROUNDED,
                    isFromScanner = true
                )
            }
            else -> Unit
        }
    }

    fun onRepeatFromResult() {
        val current = _uiState.value as? TerminalUiState.Result ?: return
        cancelActiveJobs()
        _uiState.value = TerminalUiState.AmountEntry(
            input = AmountValidator.clear(),
            isNoCardDemo = current.metadata.isNoCardDemo,
            direction = current.direction
        )
    }

    fun onBackPressed(): Boolean {
        return when (val current = _uiState.value) {
            is TerminalUiState.Home -> false
            is TerminalUiState.CardholderConsent -> {
                _uiState.value = TerminalUiState.AmountEntry(
                    input = AmountValidator.evaluate(formatKopecksAsEditable(current.amountKopecks)),
                    isNoCardDemo = false,
                    direction = current.direction
                )
                true
            }
            is TerminalUiState.NfcScannerReading,
            is TerminalUiState.NfcScanPreview,
            is TerminalUiState.NfcScanDetail -> {
                cancelActiveJobs()
                _uiState.value = TerminalUiState.NfcScannerHome
                true
            }
            is TerminalUiState.Error -> {
                cancelActiveJobs()
                _uiState.value = if (current.isFromScanner) {
                    TerminalUiState.NfcScannerHome
                } else {
                    TerminalUiState.Home
                }
                true
            }
            is TerminalUiState.Processing -> true
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

    private fun beginNfcWaitingIfAvailable(
        amountKopecks: Long,
        direction: PaymentDirection
    ) {
        when (_nfcStatus.value) {
            NfcHardwareStatus.NOT_SUPPORTED -> {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.NFC_NOT_SUPPORTED,
                    amountKopecks = amountKopecks,
                    direction = direction
                )
            }
            NfcHardwareStatus.DISABLED -> {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.NFC_DISABLED,
                    amountKopecks = amountKopecks,
                    direction = direction
                )
            }
            NfcHardwareStatus.ENABLED -> {
                startWaitingCountdown(amountKopecks, direction)
            }
        }
    }

    private fun startWaitingCountdown(
        amountKopecks: Long,
        direction: PaymentDirection
    ) {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.WaitingForCard(
            amountKopecks = amountKopecks,
            remainingSeconds = waitTimeoutSeconds,
            totalSeconds = waitTimeoutSeconds,
            direction = direction
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
                    amountKopecks = active.amountKopecks,
                    direction = active.direction
                )
            }
        }
    }

    private fun startScannerCountdown() {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.NfcScannerReading(
            remainingSeconds = waitTimeoutSeconds,
            totalSeconds = waitTimeoutSeconds
        )

        countdownJob = viewModelScope.launch {
            var remaining = waitTimeoutSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                val active = _uiState.value
                if (active !is TerminalUiState.NfcScannerReading) return@launch
                if (remaining > 0) {
                    _uiState.value = active.copy(remainingSeconds = remaining)
                }
            }
            if (_uiState.value is TerminalUiState.NfcScannerReading) {
                _feedbackEvents.tryEmit(FeedbackEvent.ERROR)
                _uiState.value = TerminalUiState.Error(
                    error = TerminalError.TIMEOUT,
                    isFromScanner = true
                )
            }
        }
    }

    private fun startProcessing(
        amountKopecks: Long,
        metadata: CardPublicMetadata,
        direction: PaymentDirection
    ) {
        cancelActiveJobs()
        _uiState.value = TerminalUiState.Processing(
            amountKopecks = amountKopecks,
            metadata = metadata,
            direction = direction
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
                    isNoCardDemo = metadata.isNoCardDemo,
                    direction = direction
                )
            )
            _uiState.value = TerminalUiState.Result(
                amountKopecks = amountKopecks,
                metadata = metadata,
                timestampMillis = timestamp,
                direction = direction
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
        private val historyRepository: HistoryRepository,
        private val nfcScanRepository: NfcScanRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TerminalViewModel(
                settingsRepository = settingsRepository,
                historyRepository = historyRepository,
                nfcScanRepository = nfcScanRepository
            ) as T
        }
    }

    companion object {
        const val DEFAULT_PROCESSING_DURATION_MS = 1600L
    }
}
