package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TelemetryCard(
    title: String,
    value: String,
    unit: String = "",
    icon: ImageVector? = null,
    accentColor: Color = SignalGreen,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .semantics { contentDescription = "$title: $value $unit" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PulsingDot(
    isActive: Boolean,
    activeColor: Color = SignalGreen,
    inactiveColor: Color = Color(0xFF4A5568),
    modifier: Modifier = Modifier
) {
    if (isActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = 0.25f))
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(activeColor)
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(inactiveColor)
        )
    }
}

@Composable
fun PingLineChart(
    history: List<Long>,
    modifier: Modifier = Modifier,
    lineColor: Color = SignalGreen
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .background(DeepNavy.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        if (history.size < 2) {
            // Draw empty grid lines
            drawLine(
                color = CardBorder,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 1.dp.toPx()
            )
            return@Canvas
        }

        val maxVal = (history.maxOrNull()?.toFloat() ?: 100f).coerceAtLeast(60f)
        val minVal = 0f
        val range = maxVal - minVal

        val stepX = size.width / (history.size - 1)
        val points = history.mapIndexed { index, value ->
            val x = index * stepX
            val normalizedY = ((value - minVal) / range).coerceIn(0f, 1f)
            val y = size.height - (normalizedY * size.height)
            Offset(x, y)
        }

        // Draw grid
        drawLine(
            color = CardBorder.copy(alpha = 0.5f),
            start = Offset(0f, size.height * 0.25f),
            end = Offset(size.width, size.height * 0.25f),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = CardBorder.copy(alpha = 0.5f),
            start = Offset(0f, size.height * 0.75f),
            end = Offset(size.width, size.height * 0.75f),
            strokeWidth = 1.dp.toPx()
        )

        // Draw Path
        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, pt ->
            if (i == 0) {
                path.moveTo(pt.x, pt.y)
                fillPath.moveTo(pt.x, size.height)
                fillPath.lineTo(pt.x, pt.y)
            } else {
                path.lineTo(pt.x, pt.y)
                fillPath.lineTo(pt.x, pt.y)
            }
        }
        fillPath.lineTo(points.last().x, size.height)
        fillPath.close()

        // Gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
            )
        )

        // Line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // End point dot
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = points.last()
        )
    }
}

@Composable
fun SpeedometerGauge(
    speedMbps: Double,
    maxSpeed: Double = 150.0,
    progress: Float = 0f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(220.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2 - 20.dp.toPx()

        val startAngle = 140f
        val sweepAngle = 260f

        // Track Arc
        drawArc(
            color = CardBorder,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
        )

        // Active Arc
        val ratio = (speedMbps / maxSpeed).coerceIn(0.0, 1.0).toFloat()
        val activeSweep = sweepAngle * ratio

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(SignalCyan, SignalGreen, SignalRed),
                center = center
            ),
            startAngle = startAngle,
            sweepAngle = activeSweep,
            useCenter = false,
            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
        )

        // Needle
        val currentAngleRad = Math.toRadians((startAngle + activeSweep).toDouble())
        val needleLength = radius - 10.dp.toPx()
        val needleEnd = Offset(
            (center.x + needleLength * cos(currentAngleRad)).toFloat(),
            (center.y + needleLength * sin(currentAngleRad)).toFloat()
        )

        drawLine(
            color = Color.White,
            start = center,
            end = needleEnd,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center hub
        drawCircle(
            color = DeepNavy,
            radius = 12.dp.toPx(),
            center = center
        )
        drawCircle(
            color = SignalGreen,
            radius = 6.dp.toPx(),
            center = center
        )
    }
}
