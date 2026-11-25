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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nfcpay.simulator.R
import com.nfcpay.simulator.domain.AmountValidator
import com.nfcpay.simulator.ui.components.TerminalSecondaryButton
import com.nfcpay.simulator.ui.components.TerminalTopBar
import com.nfcpay.simulator.ui.theme.AmberWarning
import com.nfcpay.simulator.ui.theme.EmeraldAccent
import com.nfcpay.simulator.ui.theme.EmeraldContainer
import com.nfcpay.simulator.ui.theme.GraphiteCard
import com.nfcpay.simulator.ui.theme.GraphiteElevated
import com.nfcpay.simulator.ui.theme.GraphiteOutline
import com.nfcpay.simulator.ui.theme.TextPrimary
import com.nfcpay.simulator.ui.theme.TextSecondary

@Composable
fun WaitingForCardScreen(
    amountKopecks: Long,
    remainingSeconds: Int,
    totalSeconds: Int,
    onCancelClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfcPulse")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(900)
        ),
        label = "pulse2"
    )

    val progressTarget = (remainingSeconds.toFloat() / totalSeconds.coerceAtLeast(1).toFloat())
        .coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 450, easing = LinearEasing),
        label = "countdownProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TerminalTopBar(
            title = "Ожидание карты",
            onBackClick = onCancelClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Amount display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "СУММА К ОПЛАТЕ",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AmountValidator.formatRubles(amountKopecks),
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            // Animated NFC radar rings + tap_card illustration
            Box(
                modifier = Modifier.size(216.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxRadius = size.minDimension / 2f
                    val minRadius = maxRadius * 0.45f

                    val r1 = minRadius + (maxRadius - minRadius) * pulse1
                    drawCircle(
                        color = EmeraldAccent.copy(alpha = (1f - pulse1) * 0.45f),
                        radius = r1,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    val r2 = minRadius + (maxRadius - minRadius) * pulse2
                    drawCircle(
                        color = EmeraldAccent.copy(alpha = (1f - pulse2) * 0.35f),
                        radius = r2,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.ill_tap_card),
                    contentDescription = "Поднесите бесконтактную карту",
                    modifier = Modifier.size(168.dp)
                )
            }

            Text(
                text = "Поднесите бесконтактную карту",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            // 30-second countdown block
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
                            text = "30 секунд для прикладывания карты",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Surface(
                            color = if (remainingSeconds <= 7) {
                                AmberWarning.copy(alpha = 0.16f)
                            } else {
                                EmeraldContainer
                            },
                            shape = CircleShape
                        ) {
                            Text(
                                text = "$remainingSeconds сек.",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (remainingSeconds <= 7) AmberWarning else EmeraldAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (remainingSeconds <= 7) AmberWarning else EmeraldAccent,
                        trackColor = GraphiteElevated
                    )
                }
            }

            // Antenna placement hint
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = GraphiteCard,
                border = BorderStroke(1.dp, GraphiteOutline)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_phone),
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Расположение NFC-антенны: приложите карту к верхней или центральной части задней панели устройства и удерживайте 1–2 секунды.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            TerminalSecondaryButton(
                text = "Отмена",
                onClick = onCancelClick,
                iconRes = R.drawable.ic_close
            )
        }
    }
}
