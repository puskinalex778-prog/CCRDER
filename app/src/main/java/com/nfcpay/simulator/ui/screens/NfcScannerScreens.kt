package com.nfcpay.simulator.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nfcpay.simulator.R
import com.nfcpay.simulator.domain.model.NfcHardwareStatus
import com.nfcpay.simulator.domain.model.NfcScanRecord
import com.nfcpay.simulator.ui.components.NfcStatusPill
import com.nfcpay.simulator.ui.components.TerminalPrimaryButton
import com.nfcpay.simulator.ui.components.TerminalSecondaryButton
import com.nfcpay.simulator.ui.components.TerminalTopBar
import com.nfcpay.simulator.ui.theme.AmberWarning
import com.nfcpay.simulator.ui.theme.EmeraldAccent
import com.nfcpay.simulator.ui.theme.EmeraldContainer
import com.nfcpay.simulator.ui.theme.ErrorSoft
import com.nfcpay.simulator.ui.theme.GraphiteCard
import com.nfcpay.simulator.ui.theme.GraphiteElevated
import com.nfcpay.simulator.ui.theme.GraphiteOutline
import com.nfcpay.simulator.ui.theme.TextMuted
import com.nfcpay.simulator.ui.theme.TextPrimary
import com.nfcpay.simulator.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NfcScannerHomeScreen(
    nfcStatus: NfcHardwareStatus,
    savedScans: List<NfcScanRecord>,
    onStartScanClick: () -> Unit,
    onOpenScanDetail: (NfcScanRecord) -> Unit,
    onClearAllScans: () -> Unit,
    onBackClick: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TerminalTopBar(
            title = "Сканер NFC-данных",
            onBackClick = onBackClick,
            trailingAction = {
                if (savedScans.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash),
                            contentDescription = "Очистить сохранённые метки",
                            tint = ErrorSoft,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Чтение открытых данных меток и карт",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                NfcStatusPill(status = nfcStatus)
            }

            TerminalPrimaryButton(
                text = "Считать NFC-метку / карту",
                onClick = onStartScanClick,
                iconRes = R.drawable.ic_nfc
            )

            Surface(
                color = GraphiteCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GraphiteOutline)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
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
                        text = "Сохраняются только открытые технические данные (UID, технологии, NDEF, AID). Номер карты и реквизиты не сохраняются.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        if (savedScans.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ill_empty_history),
                    contentDescription = "Нет сохранённых меток",
                    modifier = Modifier.size(144.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Сохранённых NFC-записей нет",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Нажмите «Считать NFC-метку / карту», поднесите метку и сохраните открытые данные для просмотра.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "СОХРАНЁННЫЕ ЗАПИСИ (${savedScans.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(items = savedScans, key = { it.id }) { scan ->
                    SavedScanListCard(
                        record = scan,
                        onClick = { onOpenScanDetail(scan) }
                    )
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
            title = { Text("Удалить все NFC-записи?") },
            text = { Text("Все сохранённые сканирования NFC-меток будут удалены с устройства.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAllScans()
                    }
                ) {
                    Text("Удалить", color = ErrorSoft, fontWeight = FontWeight.SemiBold)
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
private fun SavedScanListCard(
    record: NfcScanRecord,
    onClick: () -> Unit
) {
    val formattedDate = remember(record.timestampMillis) {
        SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.forLanguageTag("ru-RU"))
            .format(Date(record.timestampMillis))
    }

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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (record.applicationLabel != null) R.drawable.ic_card else R.drawable.ic_nfc
                    ),
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1
                )
                val subtitle = buildString {
                    if (record.uidHex != null) append("UID: ${record.uidHex}")
                    if (record.ndefRecords.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append("NDEF: ${record.ndefRecords.size}")
                    } else if (record.technologies.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append(record.technologies.take(2).joinToString(", "))
                    }
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun NfcScannerReadingScreen(
    remainingSeconds: Int,
    totalSeconds: Int,
    onCancelClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scannerPulse")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scannerPulse1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(900)
        ),
        label = "scannerPulse2"
    )
    val progress by animateFloatAsState(
        targetValue = (remainingSeconds.toFloat() / totalSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = LinearEasing),
        label = "scannerCountdown"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TerminalTopBar(
            title = "Считывание NFC",
            onBackClick = onCancelClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier.size(216.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxRadius = size.minDimension / 2f
                    val minRadius = maxRadius * 0.45f
                    drawCircle(
                        color = EmeraldAccent.copy(alpha = (1f - pulse1) * 0.45f),
                        radius = minRadius + (maxRadius - minRadius) * pulse1,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = EmeraldAccent.copy(alpha = (1f - pulse2) * 0.35f),
                        radius = minRadius + (maxRadius - minRadius) * pulse2,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.ill_tap_card),
                    contentDescription = "Поднесите NFC-метку или карту",
                    modifier = Modifier.size(168.dp)
                )
            }

            Text(
                text = "Поднесите NFC-метку или карту",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Будут считаны открытые данные: UID, технологии, записи NDEF (текст, ссылки) и открытые метки EMV.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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
                            text = "30 секунд для считывания",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "$remainingSeconds сек.",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (remainingSeconds <= 7) AmberWarning else EmeraldAccent
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (remainingSeconds <= 7) AmberWarning else EmeraldAccent,
                        trackColor = GraphiteElevated
                    )
                }
            }

            TerminalSecondaryButton(
                text = "Отмена",
                onClick = onCancelClick,
                iconRes = R.drawable.ic_close
            )
        }
    }
}

@Composable
fun NfcScanPreviewScreen(
    draft: NfcScanRecord,
    onSaveClick: (title: String, note: String) -> Unit,
    onRescanClick: () -> Unit,
    onDiscardClick: () -> Unit
) {
    var title by rememberSaveable(draft.id) { mutableStateOf(draft.title) }
    var note by rememberSaveable(draft.id) { mutableStateOf(draft.note) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TerminalTopBar(
            title = "Данные считаны",
            onBackClick = onDiscardClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Название записи") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldAccent,
                    unfocusedBorderColor = GraphiteOutline,
                    focusedLabelColor = EmeraldAccent,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = GraphiteCard,
                    unfocusedContainerColor = GraphiteCard
                )
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(200) },
                label = { Text("Заметка (необязательно)") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldAccent,
                    unfocusedBorderColor = GraphiteOutline,
                    focusedLabelColor = EmeraldAccent,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = GraphiteCard,
                    unfocusedContainerColor = GraphiteCard
                )
            )

            NfcScanMetadataCards(record = draft)

            Spacer(modifier = Modifier.height(4.dp))

            TerminalPrimaryButton(
                text = "Сохранить запись",
                onClick = { onSaveClick(title, note) },
                iconRes = R.drawable.ic_check
            )

            TerminalSecondaryButton(
                text = "Считать другую метку",
                onClick = onRescanClick,
                iconRes = R.drawable.ic_retry
            )

            TextButton(
                onClick = onDiscardClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Не сохранять", color = TextSecondary)
            }
        }
    }
}

@Composable
fun NfcScanDetailScreen(
    record: NfcScanRecord,
    onDeleteClick: () -> Unit,
    onRescanClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val formattedDate = remember(record.timestampMillis) {
        SimpleDateFormat("dd.MM.yyyy, HH:mm:ss", Locale.forLanguageTag("ru-RU"))
            .format(Date(record.timestampMillis))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TerminalTopBar(
            title = "Просмотр NFC-записи",
            onBackClick = onBackClick,
            trailingAction = {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trash),
                        contentDescription = "Удалить запись",
                        tint = ErrorSoft,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = GraphiteCard,
                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Сохранено: $formattedDate",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    if (record.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = record.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            NfcScanMetadataCards(record = record)

            Spacer(modifier = Modifier.height(4.dp))

            TerminalPrimaryButton(
                text = "Считать новую метку",
                onClick = onRescanClick,
                iconRes = R.drawable.ic_nfc
            )

            TerminalSecondaryButton(
                text = "К списку записей",
                onClick = onBackClick,
                iconRes = R.drawable.ic_back
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = GraphiteCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Удалить запись?") },
            text = { Text("Запись «${record.title}» будет удалена из памяти устройства.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    }
                ) {
                    Text("Удалить", color = ErrorSoft, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена", color = TextPrimary)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NfcScanMetadataCards(record: NfcScanRecord) {
    // Primary identifiers & technologies
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = GraphiteCard,
        border = BorderStroke(1.dp, GraphiteOutline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ОСНОВНЫЕ ОТКРЫТЫЕ ДАННЫЕ",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldAccent
            )

            if (record.uidHex != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "UID метки",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = record.uidHex,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                        color = TextPrimary
                    )
                }
            }

            if (record.applicationLabel != null) {
                HorizontalDivider(color = GraphiteOutline.copy(alpha = 0.5f))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Метка приложения (EMV)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = record.applicationLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }

            if (record.technologies.isNotEmpty()) {
                HorizontalDivider(color = GraphiteOutline.copy(alpha = 0.5f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Технологии NFC",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        record.technologies.forEach { tech ->
                            Surface(
                                color = EmeraldContainer,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = tech,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = EmeraldAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (record.publicAids.isNotEmpty()) {
                HorizontalDivider(color = GraphiteOutline.copy(alpha = 0.5f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Открытые идентификаторы AID",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    record.publicAids.forEach { aid ->
                        Text(
                            text = aid,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }

    // NDEF records if present
    if (record.ndefRecords.isNotEmpty()) {
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
                Text(
                    text = "ЗАПИСИ NDEF (${record.ndefRecords.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldAccent
                )
                record.ndefRecords.forEachIndexed { idx, rec ->
                    if (idx > 0) {
                        HorizontalDivider(color = GraphiteOutline.copy(alpha = 0.5f))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = rec.recordType,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = rec.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }

    // Technical parameters summary
    if (record.technicalSummary.isNotBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = GraphiteCard,
            border = BorderStroke(1.dp, GraphiteOutline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "ТЕХНИЧЕСКИЕ ПАРАМЕТРЫ",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldAccent
                )
                Text(
                    text = record.technicalSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
