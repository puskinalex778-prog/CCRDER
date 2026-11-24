package com.nfcpay.simulator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nfcpay.simulator.R
import com.nfcpay.simulator.domain.AmountValidator
import com.nfcpay.simulator.domain.model.AmountInputState
import com.nfcpay.simulator.domain.model.AmountValidationError
import com.nfcpay.simulator.domain.model.PaymentDirection
import com.nfcpay.simulator.ui.components.TerminalPrimaryButton
import com.nfcpay.simulator.ui.components.TerminalTopBar
import com.nfcpay.simulator.ui.theme.AmberWarning
import com.nfcpay.simulator.ui.theme.EmeraldAccent
import com.nfcpay.simulator.ui.theme.EmeraldContainer
import com.nfcpay.simulator.ui.theme.GraphiteCard
import com.nfcpay.simulator.ui.theme.GraphiteElevated
import com.nfcpay.simulator.ui.theme.GraphiteOutline
import com.nfcpay.simulator.ui.theme.TextMuted
import com.nfcpay.simulator.ui.theme.TextPrimary
import com.nfcpay.simulator.ui.theme.TextSecondary

@Composable
fun AmountEntryScreen(
    input: AmountInputState,
    isNoCardDemo: Boolean,
    direction: PaymentDirection,
    onDigitClick: (Char) -> Unit,
    onDecimalClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val isSendMode = direction == PaymentDirection.SEND
    val displayNumber = AmountValidator.formatLiveInput(input.rawInput)
    val prefix = if (isSendMode && input.rawInput.isNotEmpty()) "+ " else ""
    val fullDisplay = "$prefix$displayNumber\u00A0₽"

    val fontSize = when {
        fullDisplay.length <= 10 -> 46.sp
        fullDisplay.length <= 14 -> 36.sp
        else -> 28.sp
    }

    val hintText = when {
        input.isValid && isSendMode -> "Нажмите «Отправить» и приложите карту"
        input.isValid -> "Сумма готова к демонстрации"
        input.rawInput.isEmpty() && isSendMode -> "Укажите сумму для зачисления на карту"
        input.rawInput.isEmpty() -> "Введите сумму больше 0 ₽"
        input.validationError == AmountValidationError.ZERO_OR_NEGATIVE ->
            "Сумма должна быть больше нуля"
        else -> "Допустимо не более двух знаков после запятой"
    }

    val topTitle = when {
        isNoCardDemo -> "Демо без NFC"
        isSendMode -> "Зачислить на карту"
        else -> "Сумма платежа"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TerminalTopBar(
            title = topTitle,
            onBackClick = onBackClick,
            trailingAction = {
                if (input.rawInput.isNotEmpty()) {
                    TextButton(onClick = onClearClick) {
                        Text(
                            text = "Сброс",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isNoCardDemo) {
                Surface(
                    color = EmeraldContainer,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "Без считывания карты",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = EmeraldAccent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            } else if (isSendMode) {
                Surface(
                    color = EmeraldContainer,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "Обратная отправка · Демо-зачисление",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = EmeraldAccent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Large ruble display card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = GraphiteCard,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (input.isValid) EmeraldAccent.copy(alpha = 0.45f) else GraphiteOutline
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isSendMode) "ЗАЧИСЛИТЬ НА КАРТУ (РУБЛИ)" else "К ОПЛАТЕ (РУБЛИ)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    Text(
                        text = fullDisplay,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = fontSize,
                            lineHeight = (fontSize.value + 6).sp
                        ),
                        color = when {
                            input.rawInput.isEmpty() -> TextMuted
                            isSendMode -> EmeraldAccent
                            else -> TextPrimary
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            input.isValid -> EmeraldAccent
                            input.rawInput.isNotEmpty() -> AmberWarning
                            else -> TextSecondary
                        }
                    )
                }
            }

            // Numeric Keypad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeypadRow(
                    keys = listOf("1", "2", "3"),
                    onKeyClick = { onDigitClick(it[0]) }
                )
                KeypadRow(
                    keys = listOf("4", "5", "6"),
                    onKeyClick = { onDigitClick(it[0]) }
                )
                KeypadRow(
                    keys = listOf("7", "8", "9"),
                    onKeyClick = { onDigitClick(it[0]) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KeypadButton(
                        label = ",",
                        onClick = onDecimalClick,
                        modifier = Modifier.weight(1f)
                    )
                    KeypadButton(
                        label = "0",
                        onClick = { onDigitClick('0') },
                        modifier = Modifier.weight(1f)
                    )
                    KeypadBackspaceButton(
                        onClick = onBackspaceClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            TerminalPrimaryButton(
                text = if (isSendMode) "Отправить" else "Продолжить",
                onClick = onContinueClick,
                enabled = input.isValid,
                iconRes = R.drawable.ic_arrow_right
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun KeypadRow(
    keys: List<String>,
    onKeyClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        keys.forEach { key ->
            KeypadButton(
                label = key,
                onClick = { onKeyClick(key) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = GraphiteCard,
        border = BorderStroke(1.dp, GraphiteOutline.copy(alpha = 0.7f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun KeypadBackspaceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = GraphiteElevated,
        border = BorderStroke(1.dp, GraphiteOutline.copy(alpha = 0.7f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "Удалить символ",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
