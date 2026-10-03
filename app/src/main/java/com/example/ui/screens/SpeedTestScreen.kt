package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SpeedTestRecord
import com.example.engine.SpeedTestPhase
import com.example.engine.SpeedTestProgress
import com.example.ui.components.SpeedometerGauge
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpeedTestScreen(
    testProgress: SpeedTestProgress,
    testHistory: List<SpeedTestRecord>,
    onStartTest: () -> Unit,
    onClearHistory: () -> Unit
) {
    val phaseText = when (testProgress.phase) {
        SpeedTestPhase.IDLE -> "Siap Uji Kecepatan"
        SpeedTestPhase.PING -> "Mengukur Latensi & Jitter..."
        SpeedTestPhase.DOWNLOAD -> "Menguji Kecepatan Unduh (Download)..."
        SpeedTestPhase.UPLOAD -> "Menguji Kecepatan Unggah (Upload)..."
        SpeedTestPhase.FINISHED -> "Pengujian Selesai"
        SpeedTestPhase.ERROR -> "Pengujian Terkendala"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp)
            .testTag("speed_test_screen")
    ) {
        item {
            Text(
                text = "Test Speed Jaringan",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Ukur kecepatan unduh dan unggah secara langsung & real-time",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gauge Card
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
                    Text(
                        text = phaseText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (testProgress.isRunning) SignalGreen else TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(contentAlignment = Alignment.Center) {
                        val displaySpeed = if (testProgress.isRunning) {
                            testProgress.currentSpeedMbps
                        } else {
                            testProgress.downloadFinalMbps
                        }

                        SpeedometerGauge(
                            speedMbps = displaySpeed,
                            progress = testProgress.progress
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.US, "%.1f", displaySpeed),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Mbps",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SignalCyan
                            )
                        }
                    }

                    if (testProgress.isRunning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { testProgress.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = SignalGreen,
                            trackColor = CardBorder,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Metric Badges: Ping, Download, Upload
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPill(
                            title = "Ping",
                            value = "${testProgress.pingMs} ms",
                            icon = Icons.Default.Bolt,
                            color = SignalGreen
                        )
                        MetricPill(
                            title = "Unduh",
                            value = "${String.format(Locale.US, "%.1f", testProgress.downloadFinalMbps)} Mbps",
                            icon = Icons.Default.ArrowDownward,
                            color = SignalCyan
                        )
                        MetricPill(
                            title = "Unggah",
                            value = "${String.format(Locale.US, "%.1f", testProgress.uploadFinalMbps)} Mbps",
                            icon = Icons.Default.ArrowUpward,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onStartTest,
                        enabled = !testProgress.isRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SignalGreen,
                            contentColor = DeepNavy
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_speed_test_button")
                    ) {
                        Icon(
                            imageVector = if (testProgress.isRunning) Icons.Default.NetworkCheck else Icons.Default.PlayArrow,
                            contentDescription = "Mulai Test",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (testProgress.isRunning) "Sedang Menguji..." else "Mulai Tes Kecepatan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // History Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RIWAYAT PENGUJIAN TERSIMPAN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                if (testHistory.isNotEmpty()) {
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Riwayat",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (testHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada riwayat uji kecepatan",
                            fontSize = 13.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
        } else {
            items(testHistory) { record ->
                val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(record.timestamp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${record.networkType} • $dateStr",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "↓ ${String.format(Locale.US, "%.1f", record.downloadMbps)} Mbps",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SignalGreen
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "↑ ${String.format(Locale.US, "%.1f", record.uploadMbps)} Mbps",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SignalCyan
                                )
                            }
                        }

                        Text(
                            text = "⚡ ${record.pingMs} ms",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricPill(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = title, fontSize = 11.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
