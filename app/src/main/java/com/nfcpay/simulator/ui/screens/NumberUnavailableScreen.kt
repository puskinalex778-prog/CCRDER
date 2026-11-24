package com.nfcpay.simulator.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nfcpay.simulator.R
import com.nfcpay.simulator.domain.AmountValidator
import com.nfcpay.simulator.domain.model.CardPublicMetadata
import com.nfcpay.simulator.domain.model.PaymentDirection
import com.nfcpay.simulator.ui.components.TerminalPrimaryButton
import com.nfcpay.simulator.ui.components.TerminalSecondaryButton
import com.nfcpay.simulator.ui.components.TerminalTopBar
import com.nfcpay.simulator.ui.theme.EmeraldAccent
import com.nfcpay.simulator.ui.theme.GraphiteCard
import com.nfcpay.simulator.ui.theme.GraphiteOutline
import com.nfcpay.simulator.ui.theme.TextPrimary
import com.nfcpay.simulator.ui.theme.TextSecondary

@Composable
fun NumberUnavailableScreen(
    amountKopecks: Long,
    metadata: CardPublicMetadata,
    direction: PaymentDirection,
    onContinueWithoutNumberClick: () -> Unit,
    onRetryClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val isSendMode = direction == PaymentDirection.SEND
    val formattedAmount = AmountValidator.formatRubles(amountKopecks)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TerminalTopBar(
            title = "Карта обнаружена",
            onBackClick = onCancelClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ill_error),
                contentDescription = null,
                modifier = Modifier.size(144.dp)
            )

            Text(
                text = "Карта обнаружена. Маскированный номер недоступен",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Не все бесконтактные карты отдают последние цифры номера через открытые NFC-записи. " +
                    "Вы можете продолжить демонстрацию без реквизитов карты.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = GraphiteCard,
                border = BorderStroke(1.dp, GraphiteOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isSendMode) "К зачислению" else "Сумма",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = if (isSendMode) "+ $formattedAmount" else formattedAmount,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSendMode) EmeraldAccent else TextPrimary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_card),
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = metadata.toOpenHistoryDescription(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TerminalPrimaryButton(
                    text = "Продолжить без реквизитов",
                    onClick = onContinueWithoutNumberClick,
                    iconRes = R.drawable.ic_arrow_right
                )
                TerminalSecondaryButton(
                    text = "Повторить считывание",
                    onClick = onRetryClick,
                    iconRes = R.drawable.ic_retry
                )
                TextButton(onClick = onCancelClick) {
                    Text(
                        text = "Отмена",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
