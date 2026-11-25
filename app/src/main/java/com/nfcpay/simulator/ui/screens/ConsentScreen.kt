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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nfcpay.simulator.R
import com.nfcpay.simulator.domain.AmountValidator
import com.nfcpay.simulator.ui.components.TerminalPrimaryButton
import com.nfcpay.simulator.ui.components.TerminalSecondaryButton
import com.nfcpay.simulator.ui.components.TerminalTopBar
import com.nfcpay.simulator.ui.theme.EmeraldAccent
import com.nfcpay.simulator.ui.theme.EmeraldContainer
import com.nfcpay.simulator.ui.theme.GraphiteCard
import com.nfcpay.simulator.ui.theme.GraphiteElevated
import com.nfcpay.simulator.ui.theme.GraphiteOutline
import com.nfcpay.simulator.ui.theme.TextPrimary
import com.nfcpay.simulator.ui.theme.TextSecondary

@Composable
fun ConsentScreen(
    amountKopecks: Long,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TerminalTopBar(
            title = "Согласие владельца карты",
            onBackClick = onCancelClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ill_privacy),
                contentDescription = null,
                modifier = Modifier.size(136.dp)
            )

            Surface(
                color = GraphiteElevated,
                shape = CircleShape,
                border = BorderStroke(1.dp, GraphiteOutline)
            ) {
                Text(
                    text = "Сумма: ${AmountValidator.formatRubles(amountKopecks)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = GraphiteCard,
                border = BorderStroke(1.dp, GraphiteOutline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Для проведения бесконтактной операции могут быть прочитаны доступные через NFC данные вашей карты. " +
                            "На экране будут показаны только последние четыре цифры номера, если они доступны.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )

                    Surface(
                        color = EmeraldContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_shield),
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Считывание NFC выключено до явного подтверждения владельца карты.",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldAccent
                            )
                        }
                    }
                }
            }

            Text(
                text = "Подтвердите согласие кнопкой ниже. Само прикладывание карты не считается согласием.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TerminalPrimaryButton(
                    text = "Разрешаю чтение моей карты",
                    onClick = onConfirmClick,
                    iconRes = R.drawable.ic_check
                )
                TerminalSecondaryButton(
                    text = "Отмена",
                    onClick = onCancelClick,
                    iconRes = R.drawable.ic_close
                )
            }
        }
    }
}
