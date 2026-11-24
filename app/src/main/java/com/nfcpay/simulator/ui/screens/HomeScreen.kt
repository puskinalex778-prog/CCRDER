package com.nfcpay.simulator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nfcpay.simulator.R
import com.nfcpay.simulator.domain.model.AppSettings
import com.nfcpay.simulator.domain.model.NfcHardwareStatus
import com.nfcpay.simulator.ui.components.NfcPayLogo
import com.nfcpay.simulator.ui.components.NfcStatusPill
import com.nfcpay.simulator.ui.components.TerminalPrimaryButton
import com.nfcpay.simulator.ui.components.TerminalSecondaryButton
import com.nfcpay.simulator.ui.theme.AmberWarning
import com.nfcpay.simulator.ui.theme.EmeraldAccent
import com.nfcpay.simulator.ui.theme.EmeraldContainer
import com.nfcpay.simulator.ui.theme.GraphiteBg
import com.nfcpay.simulator.ui.theme.GraphiteCard
import com.nfcpay.simulator.ui.theme.GraphiteElevated
import com.nfcpay.simulator.ui.theme.GraphiteOutline
import com.nfcpay.simulator.ui.theme.OnEmerald
import com.nfcpay.simulator.ui.theme.TextMuted
import com.nfcpay.simulator.ui.theme.TextPrimary
import com.nfcpay.simulator.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    nfcStatus: NfcHardwareStatus,
    settings: AppSettings,
    historyCount: Int,
    savedScansCount: Int,
    onReceivePaymentClick: () -> Unit,
    onSendToCardClick: () -> Unit,
    onDemoWithoutNfcClick: () -> Unit,
    onOpenNfcScannerClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onOpenNfcSettings: () -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onVibrationToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header with brand logo and live NFC status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NfcPayLogo()
            NfcStatusPill(status = nfcStatus)
        }

        // Hero terminal banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(22.dp),
            color = GraphiteCard,
            border = BorderStroke(1.dp, GraphiteOutline)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_terminal),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    GraphiteBg.copy(alpha = 0.94f),
                                    GraphiteBg.copy(alpha = 0.72f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = EmeraldContainer.copy(alpha = 0.9f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "ДЕМО-ТЕРМИНАЛ · ОФЛАЙН",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = EmeraldAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Симулятор NFC-оплаты",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Приём, зачисление и чтение NFC без списания средств",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Quick prompt when NFC is disabled
        if (nfcStatus == NfcHardwareStatus.DISABLED) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = GraphiteCard,
                border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_warning),
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Включите NFC для чтения карт",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenNfcSettings) {
                        Text(
                            text = "Настройки",
                            color = EmeraldAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Primary actions: Receive & Reverse Send (Top-Up)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TerminalPrimaryButton(
                text = "Получить платёж",
                onClick = onReceivePaymentClick,
                iconRes = R.drawable.ic_receive
            )

            TerminalSecondaryButton(
                text = "Отправить на карту",
                onClick = onSendToCardClick,
                iconRes = R.drawable.ic_arrow_right,
                accentColor = EmeraldAccent
            )

            TerminalSecondaryButton(
                text = "Демо без NFC",
                onClick = onDemoWithoutNfcClick,
                iconRes = R.drawable.ic_demo,
                accentColor = TextPrimary
            )
        }

        // NFC Data Scanner card
        HomeMenuCard(
            iconRes = R.drawable.ic_nfc,
            title = "Сканер NFC-данных",
            subtitle = if (savedScansCount == 0) {
                "Считать и сохранить открытые данные"
            } else {
                "Сохранённых меток: $savedScansCount"
            },
            onClick = onOpenNfcScannerClick
        )

        // History navigation card
        HomeMenuCard(
            iconRes = R.drawable.ic_history,
            title = "История транзакций",
            subtitle = if (historyCount == 0) {
                "Нет сохранённых записей"
            } else {
                "Записей: $historyCount"
            },
            onClick = onHistoryClick
        )

        // Sound & Vibration settings card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = GraphiteCard,
            border = BorderStroke(1.dp, GraphiteOutline)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingToggleRow(
                    iconRes = if (settings.soundEnabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off,
                    title = "Звук",
                    subtitle = "Сигнал подтверждения операции",
                    checked = settings.soundEnabled,
                    onCheckedChange = onSoundToggle
                )
                HorizontalDivider(
                    color = GraphiteOutline.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingToggleRow(
                    iconRes = R.drawable.ic_vibration,
                    title = "Вибрация",
                    subtitle = "Тактильный отклик терминала",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = onVibrationToggle
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Развлекательный офлайн-симулятор · Интернет не используется",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun HomeMenuCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = GraphiteCard,
        border = BorderStroke(1.dp, GraphiteOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GraphiteElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (checked) EmeraldAccent else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnEmerald,
                checkedTrackColor = EmeraldAccent,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = GraphiteElevated,
                uncheckedBorderColor = GraphiteOutline
            )
        )
    }
}
