package com.nfcpay.simulator

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nfcpay.simulator.data.FeedbackManager
import com.nfcpay.simulator.data.SharedPreferencesHistoryRepository
import com.nfcpay.simulator.data.SharedPreferencesSettingsRepository
import com.nfcpay.simulator.domain.model.NfcHardwareStatus
import com.nfcpay.simulator.nfc.NfcReaderController
import com.nfcpay.simulator.ui.NfcPayApp
import com.nfcpay.simulator.ui.state.FeedbackEvent
import com.nfcpay.simulator.ui.state.TerminalUiState
import com.nfcpay.simulator.ui.viewmodel.TerminalViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val nfcReaderController = NfcReaderController()
    private lateinit var feedbackManager: FeedbackManager

    private val viewModel: TerminalViewModel by viewModels {
        TerminalViewModel.Factory(
            settingsRepository = SharedPreferencesSettingsRepository(applicationContext),
            historyRepository = SharedPreferencesHistoryRepository(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        feedbackManager = FeedbackManager(applicationContext)

        // Bind NFC Reader Mode strictly to WaitingForCard while Activity is RESUMED
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect { state ->
                    if (state is TerminalUiState.WaitingForCard) {
                        val status = nfcReaderController.startReading(this@MainActivity) { result ->
                            viewModel.onNfcTagResult(result)
                        }
                        if (status != NfcHardwareStatus.ENABLED) {
                            viewModel.updateNfcStatus(status)
                        }
                    } else {
                        nfcReaderController.stopReading(this@MainActivity)
                    }
                }
            }
        }

        // Handle sound and vibration feedback events
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.feedbackEvents.collect { event ->
                    val currentSettings = viewModel.settings.value
                    when (event) {
                        FeedbackEvent.KEY_TAP ->
                            feedbackManager.playKeyTap(currentSettings.vibrationEnabled)
                        FeedbackEvent.CARD_DETECTED ->
                            feedbackManager.playCardDetected(
                                soundEnabled = currentSettings.soundEnabled,
                                vibrationEnabled = currentSettings.vibrationEnabled
                            )
                        FeedbackEvent.SUCCESS ->
                            feedbackManager.playSuccess(
                                soundEnabled = currentSettings.soundEnabled,
                                vibrationEnabled = currentSettings.vibrationEnabled
                            )
                        FeedbackEvent.ERROR ->
                            feedbackManager.playError(
                                soundEnabled = currentSettings.soundEnabled,
                                vibrationEnabled = currentSettings.vibrationEnabled
                            )
                    }
                }
            }
        }

        setContent {
            NfcPayApp(
                viewModel = viewModel,
                onOpenNfcSettings = ::openSystemNfcSettings
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val status = nfcReaderController.checkHardwareStatus(this)
        viewModel.updateNfcStatus(status)
    }

    override fun onPause() {
        // Always stop NFC Reader Mode immediately on pause
        nfcReaderController.stopReading(this)

        // Distinguish genuine backgrounding from screen rotation
        if (!isChangingConfigurations) {
            viewModel.onAppBackgrounded()
        }
        super.onPause()
    }

    private fun openSystemNfcSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            } catch (_: ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }
}
