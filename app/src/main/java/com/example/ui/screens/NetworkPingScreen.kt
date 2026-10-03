package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NetworkTelemetry
import com.example.ui.components.PingLineChart
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

@Composable
fun NetworkPingScreen(
    networkTelemetry: NetworkTelemetry,
    isOptimizingRoute: Boolean,
    optimizationResult: String?,
    onOptimizeRoute: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("network_ping_screen")
    ) {
        Text(
            text = "Optimasi Jaringan & Ping",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Text(
            text = "Stabilisasi latensi radio modem & pemilihan rute DNS terbaik",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Latency Chart Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LATENSI KONEKSI REAL-TIME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Text(
                            text = "${networkTelemetry.currentPingMs} ms",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                networkTelemetry.currentPingMs in 1..50 -> SignalGreen
                                networkTelemetry.currentPingMs in 51..110 -> SignalAmber
                                else -> SignalRed
                            }
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Rata-rata", fontSize = 11.sp, color = TextTertiary)
                        Text(
                            text = "${networkTelemetry.avgPingMs} ms",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                PingLineChart(
                    history = networkTelemetry.pingHistory,
                    lineColor = SignalGreen
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Server Target: ${networkTelemetry.targetServer}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Kehilangan Paket: ${networkTelemetry.packetLossPercent}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (networkTelemetry.packetLossPercent > 0) SignalRed else SignalGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3 Cards: Jitter, Packet Loss, IP
        Row(modifier = Modifier.fillMaxWidth()) {
            TelemetryCard(
                title = "Jitter (Variasi)",
                value = "${networkTelemetry.jitterMs}",
                unit = "ms",
                icon = Icons.Default.Bolt,
                accentColor = SignalAmber,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            TelemetryCard(
                title = "Packet Loss",
                value = "${networkTelemetry.packetLossPercent}",
                unit = "%",
                accentColor = if (networkTelemetry.packetLossPercent == 0) SignalGreen else SignalRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Route Optimizer Button & Info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SignalCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Rute DNS",
                        tint = SignalCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Optimalisasi Jalur Rute DNS Otomatis",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Membandingkan Cloudflare, Google DNS, OpenDNS, dan Quad9 secara paralel untuk memilih latensi terendah pada kartu SIM & BTS Anda.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onOptimizeRoute,
                    enabled = !isOptimizingRoute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalCyan,
                        contentColor = DeepNavy
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("optimize_route_button")
                ) {
                    if (isOptimizingRoute) {
                        CircularProgressIndicator(
                            color = DeepNavy,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Menguji Rute DNS...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Optimasi",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Optimasi & Refresh Jalur Sekarang", fontWeight = FontWeight.Bold)
                    }
                }

                AnimatedVisibility(visible = optimizationResult != null) {
                    optimizationResult?.let { res ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "✓ $res",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SignalGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware & Network Provider Diagnostics
        Text(
            text = "DETAIL ADAPTOR JARINGAN",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                NetworkDetailRow(icon = Icons.Default.CellTower, label = "Tipe Sinyal", value = networkTelemetry.networkType)
                NetworkDetailRow(icon = Icons.Default.Router, label = "Operator / SSID", value = networkTelemetry.carrierOrSsid)
                NetworkDetailRow(icon = Icons.Default.Dns, label = "Alamat IP Lokal", value = networkTelemetry.ipAddress)
                NetworkDetailRow(icon = Icons.Default.Bolt, label = "Data Seluler Total", value = "${String.format("%.1f", networkTelemetry.totalMobileDataMb)} MB")
                NetworkDetailRow(icon = Icons.Default.Bolt, label = "Data Wi-Fi Total", value = "${String.format("%.1f", networkTelemetry.totalWifiDataMb)} MB")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NetworkDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = SignalCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
        }
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
