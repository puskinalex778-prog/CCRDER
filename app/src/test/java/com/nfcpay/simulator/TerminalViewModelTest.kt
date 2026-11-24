package com.nfcpay.simulator

import com.nfcpay.simulator.data.HistoryRepository
import com.nfcpay.simulator.data.SettingsRepository
import com.nfcpay.simulator.domain.model.AppSettings
import com.nfcpay.simulator.domain.model.CardPublicMetadata
import com.nfcpay.simulator.domain.model.DemoTransaction
import com.nfcpay.simulator.domain.model.NfcHardwareStatus
import com.nfcpay.simulator.domain.model.NfcReadResult
import com.nfcpay.simulator.ui.state.TerminalError
import com.nfcpay.simulator.ui.state.TerminalUiState
import com.nfcpay.simulator.ui.viewmodel.TerminalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsRepo: FakeSettingsRepository
    private lateinit var historyRepo: FakeHistoryRepository
    private var fakeClockTime = 1_700_000_000_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepo = FakeSettingsRepository()
        historyRepo = FakeHistoryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TerminalViewModel = TerminalViewModel(
        settingsRepository = settingsRepo,
        historyRepository = historyRepo,
        clock = { fakeClockTime },
        processingDurationMs = 1500L,
        waitTimeoutSeconds = 30
    )

    @Test
    fun `full NFC happy path enforces consent, processes card, and stores no PAN in history`() = runTest {
        val vm = createViewModel()
        vm.updateNfcStatus(NfcHardwareStatus.ENABLED)

        assertTrue(vm.uiState.value is TerminalUiState.Home)

        vm.onReceivePaymentClick()
        assertTrue(vm.uiState.value is TerminalUiState.AmountEntry)

        // Cannot continue with empty amount
        vm.onAmountContinueClick()
        assertTrue(vm.uiState.value is TerminalUiState.AmountEntry)

        // Enter 1250,50
        "1250".forEach { vm.onDigitPressed(it) }
        vm.onDecimalPressed()
        vm.onDigitPressed('5')
        vm.onDigitPressed('0')
        vm.onAmountContinueClick()

        // Must enter Consent before WaitingForCard
        val consentState = vm.uiState.value as TerminalUiState.CardholderConsent
        assertEquals(125050L, consentState.amountKopecks)

        // Unsolicited NFC tap during Consent MUST be ignored
        val sampleMeta = CardPublicMetadata(
            last4Digits = "4321",
            applicationLabel = "MIR",
            nfcTechnology = "IsoDep (EMV)"
        )
        vm.onNfcTagResult(NfcReadResult.CardRead(sampleMeta))
        assertTrue(vm.uiState.value is TerminalUiState.CardholderConsent)

        // Confirm consent -> WaitingForCard
        vm.onConsentConfirmed()
        val waiting = vm.uiState.value as TerminalUiState.WaitingForCard
        assertEquals(30, waiting.remainingSeconds)

        // Card read -> Processing
        vm.onNfcTagResult(NfcReadResult.CardRead(sampleMeta))
        assertTrue(vm.uiState.value is TerminalUiState.Processing)

        // Duplicate tap while Processing MUST be ignored
        vm.onNfcTagResult(NfcReadResult.CardRead(sampleMeta.copy(last4Digits = "9999")))
        assertTrue(vm.uiState.value is TerminalUiState.Processing)

        // Complete processing animation (1500 ms)
        advanceTimeBy(1500L)
        advanceUntilIdle()

        val result = vm.uiState.value as TerminalUiState.Result
        assertEquals(125050L, result.amountKopecks)
        assertTrue(result.metadata.hasVerifiedLast4)
        assertEquals("4321", result.metadata.last4Digits)

        // Verify history saved exactly 1 record and NEVER contains card digits
        assertEquals(1, historyRepo.history.value.size)
        val saved = historyRepo.history.value.first()
        assertEquals(125050L, saved.amountKopecks)
        assertEquals(DemoTransaction.STATUS_COMPLETED, saved.status)
        assertFalse(saved.openCardInfo.contains("4321"))
        assertFalse(saved.openCardInfo.contains("9999"))
    }

    @Test
    fun `card without readable last4 prompts user and continues without fake digits`() = runTest {
        val vm = createViewModel()
        vm.updateNfcStatus(NfcHardwareStatus.ENABLED)

        vm.onReceivePaymentClick()
        vm.onDigitPressed('5')
        vm.onDigitPressed('0')
        vm.onAmountContinueClick()
        vm.onConsentConfirmed()

        val noNumberMeta = CardPublicMetadata(
            last4Digits = null,
            applicationLabel = null,
            nfcTechnology = "NfcA"
        )
        vm.onNfcTagResult(NfcReadResult.NumberUnavailable(noNumberMeta))
        assertTrue(vm.uiState.value is TerminalUiState.NumberUnavailablePrompt)

        vm.onContinueWithoutCardNumber()
        assertTrue(vm.uiState.value is TerminalUiState.Processing)

        advanceTimeBy(1500L)
        advanceUntilIdle()

        val result = vm.uiState.value as TerminalUiState.Result
        assertFalse(result.metadata.hasVerifiedLast4)
        assertEquals(null, result.metadata.last4Digits)
    }

    @Test
    fun `demo without NFC skips consent and waiting and does not invent card digits`() = runTest {
        val vm = createViewModel()
        vm.updateNfcStatus(NfcHardwareStatus.NOT_SUPPORTED)

        vm.onDemoWithoutNfcClick()
        val entry = vm.uiState.value as TerminalUiState.AmountEntry
        assertTrue(entry.isNoCardDemo)

        vm.onDigitPressed('9')
        vm.onDigitPressed('9')
        vm.onAmountContinueClick()

        assertTrue(vm.uiState.value is TerminalUiState.Processing)

        advanceTimeBy(1500L)
        advanceUntilIdle()

        val result = vm.uiState.value as TerminalUiState.Result
        assertTrue(result.metadata.isNoCardDemo)
        assertFalse(result.metadata.hasVerifiedLast4)
        assertEquals("Без считывания карты", historyRepo.history.value.first().openCardInfo)
    }

    @Test
    fun `waiting for card times out after 30 seconds`() = runTest {
        val vm = createViewModel()
        vm.updateNfcStatus(NfcHardwareStatus.ENABLED)

        vm.onReceivePaymentClick()
        vm.onDigitPressed('1')
        vm.onDigitPressed('0')
        vm.onAmountContinueClick()
        vm.onConsentConfirmed()

        assertTrue(vm.uiState.value is TerminalUiState.WaitingForCard)

        advanceTimeBy(30_000L)
        advanceUntilIdle()

        val error = vm.uiState.value as TerminalUiState.Error
        assertEquals(TerminalError.TIMEOUT, error.error)
        assertEquals(1000L, error.amountKopecks)
    }

    @Test
    fun `backgrounding app stops NFC waiting immediately`() = runTest {
        val vm = createViewModel()
        vm.updateNfcStatus(NfcHardwareStatus.ENABLED)

        vm.onReceivePaymentClick()
        vm.onDigitPressed('7')
        vm.onAmountContinueClick()
        vm.onConsentConfirmed()
        assertTrue(vm.uiState.value is TerminalUiState.WaitingForCard)

        vm.onAppBackgrounded()
        val error = vm.uiState.value as TerminalUiState.Error
        assertEquals(TerminalError.APP_BACKGROUNDED, error.error)
    }

    @Test
    fun `disabled or missing NFC transitions cleanly to error after consent`() = runTest {
        val vm = createViewModel()
        vm.updateNfcStatus(NfcHardwareStatus.DISABLED)

        vm.onReceivePaymentClick()
        vm.onDigitPressed('2')
        vm.onDigitPressed('5')
        vm.onAmountContinueClick()
        vm.onConsentConfirmed()

        val disabledErr = vm.uiState.value as TerminalUiState.Error
        assertEquals(TerminalError.NFC_DISABLED, disabledErr.error)

        // Enabling NFC and retrying resumes waiting
        vm.updateNfcStatus(NfcHardwareStatus.ENABLED)
        vm.onRetryWaiting()
        assertTrue(vm.uiState.value is TerminalUiState.WaitingForCard)
    }

    @Test
    fun `unsupported card and interrupted read produce expected error states`() = runTest {
        val vm = createViewModel()
        vm.updateNfcStatus(NfcHardwareStatus.ENABLED)

        vm.onReceivePaymentClick()
        vm.onDigitPressed('1')
        vm.onAmountContinueClick()
        vm.onConsentConfirmed()

        vm.onNfcTagResult(NfcReadResult.UnsupportedCard)
        assertEquals(TerminalError.UNSUPPORTED_CARD, (vm.uiState.value as TerminalUiState.Error).error)

        vm.onRetryWaiting()
        vm.onNfcTagResult(NfcReadResult.ReadInterrupted)
        assertEquals(TerminalError.READ_INTERRUPTED, (vm.uiState.value as TerminalUiState.Error).error)
    }

    @Test
    fun `clearing history removes all saved items`() = runTest {
        val vm = createViewModel()
        vm.onDemoWithoutNfcClick()
        vm.onDigitPressed('5')
        vm.onAmountContinueClick()
        advanceTimeBy(1500L)
        advanceUntilIdle()

        assertEquals(1, historyRepo.history.value.size)
        vm.onClearHistoryConfirm()
        assertTrue(historyRepo.history.value.isEmpty())
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val _settings = MutableStateFlow(AppSettings())
        override val settings: StateFlow<AppSettings> = _settings.asStateFlow()
        override fun setSoundEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(soundEnabled = enabled)
        }
        override fun setVibrationEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(vibrationEnabled = enabled)
        }
    }

    private class FakeHistoryRepository : HistoryRepository {
        private val _history = MutableStateFlow<List<DemoTransaction>>(emptyList())
        override val history: StateFlow<List<DemoTransaction>> = _history.asStateFlow()
        override fun addTransaction(transaction: DemoTransaction) {
            _history.value = listOf(transaction) + _history.value
        }
        override fun clearHistory() {
            _history.value = emptyList()
        }
    }
}
