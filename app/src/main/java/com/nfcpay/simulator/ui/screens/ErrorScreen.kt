package com.nfcpay.simulator.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nfcpay.simulator.R
import com.nfcpay.simulator.ui.components.TerminalPrimaryButton
import com.nfcpay.simulator.ui.components.TerminalSecondaryButton
import com.nfcpay.simulator.ui.components.TerminalTopBar
import com.nfcpay.simulator.ui.state.TerminalError
import com.nfcpay.simulator.ui.theme.EmeraldAccent
import com.nfcpay.simulator.ui.theme.TextPrimary
import com.nfcpay.simulator.ui.theme.TextSecondary

@Composable
fun ErrorScreen(
    error: TerminalError,
    hasRetryTarget: Boolean,
    isFromScanner: Boolean,
    onOpenNfcSettings: () -> Unit,
    onRetryClick: () -> Unit,
    onDemoWithoutNfcClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    val (imageRes, title, description) = when (error) {
        TerminalError.NFC_NOT_SUPPORTED -> Triple(
            R.drawable.ill_nfc_disabled,
            "NFC не поддерживается",
            if (isFromScanner) {
                "На этом устройстве отсутствует модуль NFC, поэтому считывание меток недоступно."
            } else {
                "На этом устройстве отсутствует модуль NFC. Вы можете запустить демонстрацию в режиме «Демо без NFC»."
            }
        )
        TerminalError.NFC_DISABLED -> Triple(
            R.drawable.ill_nfc_disabled,
            "NFC выключен",
            "Включите NFC в системных настройках устройства и вернитесь в приложение."
        )
        TerminalError.UNSUPPORTED_CARD -> Triple(
            R.drawable.ill_error,
            "Метка не поддерживается",
            "Обнаруженная метка или карта не поддерживает стандартное чтение. Попробуйте другую бесконтактную карту или NFC-метку."
        )
        TerminalError.READ_INTERRUPTED -> Triple(
            R.drawable.ill_error,
            "Ошибка чтения",
            "Соединение было прервано. Удерживайте карту или метку возле NFC-антенны неподвижно 1–2 секунды."
        )
        TerminalError.TIMEOUT -> Triple(
            R.drawable.ill_error,
            "Время ожидания истекло",
            "За 30 секунд карта или метка не была приложена. Считывание NFC автоматически остановлено."
        )
        TerminalError.APP_BACKGROUNDED -> Triple(
            R.drawable.ill_error,
            "Считывание остановлено",
            "Приложение было свёрнуто, поэтому режим чтения NFC автоматически отключён."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TerminalTopBar(
            title = "Внимание",
            onBackClick = onHomeClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier.size(156.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (error) {
                    TerminalError.NFC_DISABLED -> {
                        TerminalPrimaryButton(
                            text = "Открыть настройки NFC",
                            onClick = onOpenNfcSettings,
                            iconRes = R.drawable.ic_settings
                        )
                        if (hasRetryTarget) {
                            TerminalSecondaryButton(
                                text = "Проверить снова",
                                onClick = onRetryClick,
                                iconRes = R.drawable.ic_retry
                            )
                        }
                        if (!isFromScanner) {
                            TerminalSecondaryButton(
                                text = "Демо без NFC",
                                onClick = onDemoWithoutNfcClick,
                                iconRes = R.drawable.ic_demo,
                                accentColor = EmeraldAccent
                            )
                        }
                    }
                    TerminalError.NFC_NOT_SUPPORTED -> {
                        if (!isFromScanner) {
                            TerminalPrimaryButton(
                                text = "Демо без NFC",
                                onClick = onDemoWithoutNfcClick,
                                iconRes = R.drawable.ic_demo
                            )
                        }
                    }
                    TerminalError.UNSUPPORTED_CARD,
                    TerminalError.READ_INTERRUPTED,
                    TerminalError.TIMEOUT,
                    TerminalError.APP_BACKGROUNDED -> {
                        if (hasRetryTarget) {
                            TerminalPrimaryButton(
                                text = "Повторить",
                                onClick = onRetryClick,
                                iconRes = R.drawable.ic_retry
                            )
                        }
                        if (!isFromScanner) {
                            TerminalSecondaryButton(
                                text = "Демо без NFC",
                                onClick = onDemoWithoutNfcClick,
                                iconRes = R.drawable.ic_demo,
                                accentColor = EmeraldAccent
                            )
                        }
                    }
                }

                TextButton(onClick = onHomeClick) {
                    Text(
                        text = if (isFromScanner) "Вернуться в сканер" else "На главный экран",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
