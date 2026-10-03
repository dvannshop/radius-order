package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsTelemetry
import com.example.ui.components.TelemetryCard
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GpsScreen(
    gpsTelemetry: GpsTelemetry,
    isServiceRunning: Boolean,
    onToggleService: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("gps_screen")
    ) {
        Text(
            text = "GPS Booster & Akurasi Real-Time",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Text(
            text = "Kunci satelit & akurasi tinggi tanpa putus untuk navigasi & kurir",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Radar Scanner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Radar Canvas
                RadarScanner(
                    isActive = gpsTelemetry.hasLocation,
                    bearing = gpsTelemetry.bearingDegrees
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Accuracy status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when {
                                !gpsTelemetry.hasLocation -> SignalAmber.copy(alpha = 0.15f)
                                gpsTelemetry.accuracyMeters <= 5.0f -> SignalGreen.copy(alpha = 0.15f)
                                else -> SignalCyan.copy(alpha = 0.15f)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                !gpsTelemetry.hasLocation -> SignalAmber
                                gpsTelemetry.accuracyMeters <= 5.0f -> SignalGreen
                                else -> SignalCyan
                            },
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = gpsTelemetry.accuracyQuality,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !gpsTelemetry.hasLocation -> SignalAmber
                            gpsTelemetry.accuracyMeters <= 5.0f -> SignalGreen
                            else -> SignalCyan
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Continuous GPS Lock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepNavy.copy(alpha = 0.6f))
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kunci GPS Tanpa Putus (Wake-Lock)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Mencegah chip GPS tidur saat layar mati atau ganti aplikasi",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { onToggleService() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepNavy,
                            checkedTrackColor = SignalCyan
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetry Grid
        Row(modifier = Modifier.fillMaxWidth()) {
            TelemetryCard(
                title = "Latitude",
                value = String.format(Locale.US, "%.5f", gpsTelemetry.latitude),
                icon = Icons.Default.Explore,
                accentColor = SignalCyan,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            TelemetryCard(
                title = "Longitude",
                value = String.format(Locale.US, "%.5f", gpsTelemetry.longitude),
                icon = Icons.Default.Explore,
                accentColor = SignalCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            TelemetryCard(
                title = "Kecepatan Bergerak",
                value = String.format(Locale.US, "%.1f", gpsTelemetry.speedKmh),
                unit = "km/h",
                icon = Icons.Default.Speed,
                accentColor = SignalAmber,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            TelemetryCard(
                title = "Ketinggian (Altitude)",
                value = String.format(Locale.US, "%.0f", gpsTelemetry.altitudeMeters),
                unit = "m",
                icon = Icons.Default.GpsFixed,
                accentColor = SignalGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Copy coordinates & Open in Google Maps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val clip = ClipData.newPlainText(
                        "Koordinat",
                        "${gpsTelemetry.latitude}, ${gpsTelemetry.longitude}"
                    )
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(clip)
                    Toast.makeText(context, "Koordinat disalin ke clipboard", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Salin", modifier = Modifier.size(16.dp), tint = SignalCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Salin Koordinat", fontSize = 12.sp, color = TextPrimary)
            }

            Button(
                onClick = {
                    val uri = Uri.parse("geo:${gpsTelemetry.latitude},${gpsTelemetry.longitude}?q=${gpsTelemetry.latitude},${gpsTelemetry.longitude}(Posisi Saya)")
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    try {
                        context.startActivity(mapIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Aplikasi peta tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SignalCyan, contentColor = DeepNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Map, contentDescription = "Buka Peta", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Buka di Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RadarScanner(
    isActive: Boolean,
    bearing: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )

    Canvas(modifier = modifier.size(160.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2 - 8.dp.toPx()

        // Outer & inner rings
        drawCircle(
            color = CardBorder,
            radius = radius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = CardBorder.copy(alpha = 0.6f),
            radius = radius * 0.66f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = CardBorder.copy(alpha = 0.4f),
            radius = radius * 0.33f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Crosshairs
        drawLine(
            color = CardBorder.copy(alpha = 0.5f),
            start = Offset(center.x, 8.dp.toPx()),
            end = Offset(center.x, size.height - 8.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = CardBorder.copy(alpha = 0.5f),
            start = Offset(8.dp.toPx(), center.y),
            end = Offset(size.width - 8.dp.toPx(), center.y),
            strokeWidth = 1.dp.toPx()
        )

        // Animated Radar Beam
        if (isActive) {
            val sweepRad = Math.toRadians(sweepAngle.toDouble())
            val beamEnd = Offset(
                (center.x + radius * cos(sweepRad)).toFloat(),
                (center.y + radius * sin(sweepRad)).toFloat()
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, SignalCyan.copy(alpha = 0.4f)),
                    center = center
                ),
                startAngle = sweepAngle - 50f,
                sweepAngle = 50f,
                useCenter = true
            )

            drawLine(
                color = SignalCyan,
                start = center,
                end = beamEnd,
                strokeWidth = 2.dp.toPx()
            )
        }

        // Center Blip
        drawCircle(
            color = if (isActive) SignalGreen else SignalAmber,
            radius = 6.dp.toPx(),
            center = center
        )
    }
}
