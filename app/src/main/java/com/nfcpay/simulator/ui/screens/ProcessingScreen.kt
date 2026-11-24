package com.nfcpay.simulator.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nfcpay.simulator.R
import com.nfcpay.simulator.domain.AmountValidator
import com.nfcpay.simulator.domain.model.CardPublicMetadata
import com.nfcpay.simulator.domain.model.PaymentDirection
import com.nfcpay.simulator.ui.theme.EmeraldAccent
import com.nfcpay.simulator.ui.theme.EmeraldContainer
import com.nfcpay.simulator.ui.theme.GraphiteElevated
import com.nfcpay.simulator.ui.theme.GraphiteOutline
import com.nfcpay.simulator.ui.theme.TextPrimary
import com.nfcpay.simulator.ui.theme.TextSecondary

@Composable
fun ProcessingScreen(
    amountKopecks: Long,
    metadata: CardPublicMetadata,
    direction: PaymentDirection
) {
    val isSendMode = direction == PaymentDirection.SEND
    val formattedAmount = AmountValidator.formatRubles(amountKopecks)

    val infiniteTransition = rememberInfiniteTransition(label = "processingSpin")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
            Spacer(modifier = Modifier.height(20.dp))
        }

        Box(
            modifier = Modifier.size(144.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                drawCircle(
                    color = GraphiteOutline,
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = EmeraldAccent,
                    startAngle = sweepAngle,
                    sweepAngle = 110f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_brand_mark),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = if (isSendMode) "+ $formattedAmount" else formattedAmount,
            style = MaterialTheme.typography.displayMedium,
            color = if (isSendMode) EmeraldAccent else TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isSendMode) "Зачисление на карту…" else "Обработка платежа…",
            style = MaterialTheme.typography.titleLarge,
            color = EmeraldAccent,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(5.dp)
                .clip(CircleShape),
            color = EmeraldAccent,
            trackColor = GraphiteElevated
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Офлайн-анимация терминала NFC PAY",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
