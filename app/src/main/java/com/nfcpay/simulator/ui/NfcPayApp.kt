package com.nfcpay.simulator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nfcpay.simulator.ui.screens.AmountEntryScreen
import com.nfcpay.simulator.ui.screens.ConsentScreen
import com.nfcpay.simulator.ui.screens.ErrorScreen
import com.nfcpay.simulator.ui.screens.HistoryScreen
import com.nfcpay.simulator.ui.screens.HomeScreen
import com.nfcpay.simulator.ui.screens.NumberUnavailableScreen
import com.nfcpay.simulator.ui.screens.ProcessingScreen
import com.nfcpay.simulator.ui.screens.ResultScreen
import com.nfcpay.simulator.ui.screens.WaitingForCardScreen
import com.nfcpay.simulator.ui.state.TerminalUiState
import com.nfcpay.simulator.ui.theme.GraphiteBg
import com.nfcpay.simulator.ui.theme.NfcPayTheme
import com.nfcpay.simulator.ui.viewmodel.TerminalViewModel

@Composable
fun NfcPayApp(
    viewModel: TerminalViewModel,
    onOpenNfcSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nfcStatus by viewModel.nfcStatus.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState !is TerminalUiState.Home) {
        viewModel.onBackPressed()
    }

    NfcPayTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GraphiteBg)
                .safeDrawingPadding()
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith
                        fadeOut(animationSpec = tween(160))
                },
                contentKey = { it::class },
                label = "terminalScreenTransition"
            ) { state ->
                when (state) {
                    is TerminalUiState.Home -> HomeScreen(
                        nfcStatus = nfcStatus,
                        settings = settings,
                        historyCount = history.size,
                        onReceivePaymentClick = viewModel::onReceivePaymentClick,
                        onDemoWithoutNfcClick = viewModel::onDemoWithoutNfcClick,
                        onHistoryClick = viewModel::onOpenHistoryClick,
                        onOpenNfcSettings = onOpenNfcSettings,
                        onSoundToggle = viewModel::setSoundEnabled,
                        onVibrationToggle = viewModel::setVibrationEnabled
                    )

                    is TerminalUiState.AmountEntry -> AmountEntryScreen(
                        input = state.input,
                        isNoCardDemo = state.isNoCardDemo,
                        onDigitClick = viewModel::onDigitPressed,
                        onDecimalClick = viewModel::onDecimalPressed,
                        onBackspaceClick = viewModel::onBackspacePressed,
                        onClearClick = viewModel::onClearAmountPressed,
                        onContinueClick = viewModel::onAmountContinueClick,
                        onBackClick = viewModel::onBackToHome
                    )

                    is TerminalUiState.CardholderConsent -> ConsentScreen(
                        amountKopecks = state.amountKopecks,
                        onConfirmClick = viewModel::onConsentConfirmed,
                        onCancelClick = viewModel::onConsentCancelled
                    )

                    is TerminalUiState.WaitingForCard -> WaitingForCardScreen(
                        amountKopecks = state.amountKopecks,
                        remainingSeconds = state.remainingSeconds,
                        totalSeconds = state.totalSeconds,
                        onCancelClick = viewModel::onCancelWaiting
                    )

                    is TerminalUiState.NumberUnavailablePrompt -> NumberUnavailableScreen(
                        amountKopecks = state.amountKopecks,
                        metadata = state.metadata,
                        onContinueWithoutNumberClick = viewModel::onContinueWithoutCardNumber,
                        onRetryClick = viewModel::onRetryWaiting,
                        onCancelClick = viewModel::onBackToHome
                    )

                    is TerminalUiState.Processing -> ProcessingScreen(
                        amountKopecks = state.amountKopecks,
                        metadata = state.metadata
                    )

                    is TerminalUiState.Result -> ResultScreen(
                        amountKopecks = state.amountKopecks,
                        metadata = state.metadata,
                        timestampMillis = state.timestampMillis,
                        onRepeatClick = viewModel::onRepeatFromResult,
                        onHomeClick = viewModel::onBackToHome
                    )

                    is TerminalUiState.Error -> ErrorScreen(
                        error = state.error,
                        hasAmountToRetry = state.amountKopecks != null,
                        onOpenNfcSettings = onOpenNfcSettings,
                        onRetryClick = viewModel::onRetryWaiting,
                        onDemoWithoutNfcClick = viewModel::onSwitchToNoCardDemoFromError,
                        onHomeClick = viewModel::onBackToHome
                    )

                    is TerminalUiState.History -> HistoryScreen(
                        transactions = history,
                        onClearHistory = viewModel::onClearHistoryConfirm,
                        onBackClick = viewModel::onBackToHome
                    )
                }
            }
        }
    }
}
