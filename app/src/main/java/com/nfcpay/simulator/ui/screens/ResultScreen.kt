package com.nfcpay.simulator.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nfcpay.simulator.R
import com.nfcpay.simulator.domain.AmountValidator
import com.nfcpay.simulator.domain.model.CardPublicMetadata
import com.nfcpay.simulator.ui.components.TerminalPrimaryButton
import com.nfcpay.simulator.ui.components.TerminalSecondaryButton
import com.nfcpay.simulator.ui.theme.EmeraldAccent
import com.nfcpay.simulator.ui.theme.EmeraldContainer
import com.nfcpay.simulator.ui.theme.GraphiteCard
import com.nfcpay.simulator.ui.theme.GraphiteOutline
import com.nfcpay.simulator.ui.theme.TextMuted
import com.nfcpay.simulator.ui.theme.TextPrimary
import com.nfcpay.simulator.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultScreen(
    amountKopecks: Long,
    metadata: CardPublicMetadata,
    timestampMillis: Long,
    onRepeatClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    val badgeScale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        badgeScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    val formattedDate = remember(timestampMillis) {
        val formatter = SimpleDateFormat("dd.MM.yyyy, HH:mm:ss", Locale.forLanguageTag("ru-RU"))
        formatter.format(Date(timestampMillis))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = painterResource(id = R.drawable.ill_success),
            contentDescription = "Перевод завершён",
            modifier = Modifier
                .size(164.dp)
                .scale(badgeScale.value)
        )

        Text(
            text = "Перевод завершён",
            style = MaterialTheme.typography.headlineMedium,
            color = EmeraldAccent,
            textAlign = TextAlign.Center
        )

        Text(
            text = AmountValidator.formatRubles(amountKopecks),
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        if (metadata.isNoCardDemo) {
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
        }

        // Clean summary card (explicitly NO fake bank receipt, NO transaction ID, NO export button)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = GraphiteCard,
            border = BorderStroke(1.dp, GraphiteOutline)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show "Карта •••• 1234" ONLY when 4 digits were actually read from the card
                if (metadata.hasVerifiedLast4) {
                    ResultInfoRow(
                        label = "Карта",
                        value = "Карта •••• ${metadata.last4Digits}"
                    )
                    HorizontalDivider(color = GraphiteOutline.copy(alpha = 0.6f))
                } else if (metadata.isNoCardDemo) {
                    ResultInfoRow(
                        label = "Режим",
                        value = "Без считывания карты"
                    )
                    HorizontalDivider(color = GraphiteOutline.copy(alpha = 0.6f))
                } else {
                    ResultInfoRow(
                        label = "Носитель",
                        value = metadata.toOpenHistoryDescription()
                    )
                    HorizontalDivider(color = GraphiteOutline.copy(alpha = 0.6f))
                }

                ResultInfoRow(
                    label = "Дата и время",
                    value = formattedDate
                )

                HorizontalDivider(color = GraphiteOutline.copy(alpha = 0.6f))

                Text(
                    text = "Операция выполнена в терминале NFC PAY.",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TerminalPrimaryButton(
                text = "Повторить",
                onClick = onRepeatClick,
                iconRes = R.drawable.ic_retry
            )
            TerminalSecondaryButton(
                text = "На главный экран",
                onClick = onHomeClick,
                iconRes = R.drawable.ic_back
            )
        }
    }
}

@Composable
private fun ResultInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.End
        )
    }
}
