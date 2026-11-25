package com.nfcpay.simulator.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nfcpay.simulator.R
import com.nfcpay.simulator.domain.AmountValidator
import com.nfcpay.simulator.domain.model.DemoTransaction
import com.nfcpay.simulator.ui.components.TerminalTopBar
import com.nfcpay.simulator.ui.theme.EmeraldAccent
import com.nfcpay.simulator.ui.theme.EmeraldContainer
import com.nfcpay.simulator.ui.theme.ErrorSoft
import com.nfcpay.simulator.ui.theme.GraphiteCard
import com.nfcpay.simulator.ui.theme.GraphiteOutline
import com.nfcpay.simulator.ui.theme.TextMuted
import com.nfcpay.simulator.ui.theme.TextPrimary
import com.nfcpay.simulator.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    transactions: List<DemoTransaction>,
    onClearHistory: () -> Unit,
    onBackClick: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TerminalTopBar(
            title = "История транзакций",
            onBackClick = onBackClick,
            trailingAction = {
                if (transactions.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash),
                            contentDescription = "Очистить историю",
                            tint = ErrorSoft,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        )

        if (transactions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ill_empty_history),
                    contentDescription = "История пуста",
                    modifier = Modifier.size(164.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "История пока пуста",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Здесь будут отображаться завершённые операции.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = transactions, key = { it.id }) { item ->
                    HistoryItemCard(transaction = item)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = GraphiteCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Очистить историю?") },
            text = {
                Text("Все записи истории будут удалены с устройства.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearHistory()
                    }
                ) {
                    Text(
                        text = "Очистить",
                        color = ErrorSoft,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Отмена", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
private fun HistoryItemCard(transaction: DemoTransaction) {
    var expanded by remember(transaction.id) { mutableStateOf(false) }
    val formattedDate = remember(transaction.timestampMillis) {
        val formatter = SimpleDateFormat("dd.MM.yyyy, HH:mm:ss", Locale.forLanguageTag("ru-RU"))
        formatter.format(Date(transaction.timestampMillis))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded },
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AmountValidator.formatRubles(transaction.amountKopecks),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Surface(
                    color = EmeraldContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = transaction.status,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = EmeraldAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.openCardInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = GraphiteOutline.copy(alpha = 0.6f))
                    Text(
                        text = "Открытые данные: ${transaction.openCardInfo}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Номер карты, срок действия и банковские реквизиты не сохраняются.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
