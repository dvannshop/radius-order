package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatteryTelemetry
import com.example.model.GpsTelemetry
import com.example.model.NetworkTelemetry
import com.example.model.PowerProfile
import com.example.ui.components.PingLineChart
import com.example.ui.components.PulsingDot
import com.example.ui.components.TelemetryCard
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun DashboardScreen(
    isServiceRunning: Boolean,
    networkTelemetry: NetworkTelemetry,
    gpsTelemetry: GpsTelemetry,
    batteryTelemetry: BatteryTelemetry,
    currentPowerProfile: PowerProfile,
    isHudShowing: Boolean,
    lastCleanedMb: Double?,
    onToggleService: () -> Unit,
    onSelectProfile: (PowerProfile) -> Unit,
    onToggleHud: () -> Unit,
    onCleanCacheQuick: () -> Unit,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToPing: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("dashboard_screen")
    ) {
        // App Header / Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NetPulse Pro",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Signal & GPS Active Keep-Alive",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isServiceRunning) SignalGreen.copy(alpha = 0.15f) else Color(0xFF263550))
                    .border(
                        1.dp,
                        if (isServiceRunning) SignalGreen else CardBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                PulsingDot(isActive = isServiceRunning)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isServiceRunning) "OPTIMAL AKTIF" else "SIAGA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isServiceRunning) SignalGreen else TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Master Switch Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isServiceRunning) SignalGreen else CardBorder,
                    RoundedCornerShape(20.dp)
                )
                .testTag("hero_master_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isServiceRunning) listOf(
                                SignalGreen.copy(alpha = 0.12f),
                                Color.Transparent
                            ) else listOf(Color.Transparent, Color.Transparent)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isServiceRunning) "STABILISATOR BERJALAN" else "STABILISATOR MATI",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isServiceRunning) SignalGreen else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isServiceRunning)
                                    "Ping konstan & GPS dikunci agar sinyal tidak idle"
                                else
                                    "Aktifkan untuk menjaga koneksi dan akurasi rute",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = { onToggleService() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepNavy,
                                checkedTrackColor = SignalGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = CardBorder
                            ),
                            modifier = Modifier.testTag("master_optimizer_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Ping chart teaser
                    Text(
                        text = "GRAFIK LATENSI REAL-TIME (${networkTelemetry.currentPingMs} ms)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PingLineChart(
                        history = networkTelemetry.pingHistory,
                        modifier = Modifier.clickable { onNavigateToPing() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4 Key Telemetry Cards Grid
        Row(modifier = Modifier.fillMaxWidth()) {
            TelemetryCard(
                title = "Ping Laten",
                value = "${networkTelemetry.currentPingMs}",
                unit = "ms",
                icon = Icons.Default.Bolt,
                accentColor = when {
                    networkTelemetry.currentPingMs in 1..50 -> SignalGreen
                    networkTelemetry.currentPingMs in 51..110 -> SignalAmber
                    else -> SignalRed
                },
                subtitle = "Jitter: ${networkTelemetry.jitterMs}ms",
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToPing() }
                    .testTag("ping_telemetry_card")
            )
            Spacer(modifier = Modifier.width(12.dp))
            TelemetryCard(
                title = "Akurasi GPS",
                value = if (gpsTelemetry.hasLocation) "±${String.format("%.1f", gpsTelemetry.accuracyMeters)}" else "--",
                unit = "m",
                icon = Icons.Default.LocationOn,
                accentColor = SignalCyan,
                subtitle = if (gpsTelemetry.hasLocation) "Kunci GPS Aktif" else "Mencari GPS",
                modifier = Modifier
                    .weight(1f)
                    .testTag("gps_telemetry_card")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            TelemetryCard(
                title = "Kecepatan",
                value = String.format("%.0f", gpsTelemetry.speedKmh),
                unit = "km/h",
                icon = Icons.Default.Speed,
                accentColor = SignalAmber,
                subtitle = "Alt: ${String.format("%.0f", gpsTelemetry.altitudeMeters)}m",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            TelemetryCard(
                title = "Baterai",
                value = "${batteryTelemetry.percentage}",
                unit = "%",
                icon = Icons.Default.Power,
                accentColor = if (batteryTelemetry.temperatureCelsius > 40f) SignalRed else SignalGreen,
                subtitle = "${String.format("%.1f", batteryTelemetry.temperatureCelsius)}°C | ${batteryTelemetry.health}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Traffic Stats Summary
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Jaringan",
                            tint = SignalCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${networkTelemetry.networkType} • ${networkTelemetry.carrierOrSsid}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "IP: ${networkTelemetry.ipAddress}",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Unduh Langsung", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = "${networkTelemetry.downloadSpeedKbps} KB/s",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SignalGreen
                        )
                    }
                    Column {
                        Text(text = "Unggah Langsung", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = "${networkTelemetry.uploadSpeedKbps} KB/s",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SignalCyan
                        )
                    }
                    Column {
                        Text(text = "Data Seluler Total", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = "${String.format("%.1f", networkTelemetry.totalMobileDataMb)} MB",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Power Profiles Selection
        Text(
            text = "PROFIL MODE PERFORMA & BATERAI",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PowerProfile.values().forEach { profile ->
                val isSelected = currentPowerProfile == profile
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CardSurfaceElevated else CardSurface)
                        .border(
                            1.dp,
                            if (isSelected) SignalGreen else CardBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectProfile(profile) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) SignalGreen else TextPrimary
                        )
                        Text(
                            text = profile.subtitle,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                if (isSelected) SignalGreen else CardBorder,
                                CircleShape
                            )
                            .background(if (isSelected) SignalGreen else Color.Transparent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions Row
        Text(
            text = "PINTASAN CEPAT DRIVER",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onToggleHud,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHudShowing) SignalAmber else CardSurfaceElevated,
                    contentColor = if (isHudShowing) DeepNavy else TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("floating_hud_quick_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "HUD Melayang",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (isHudShowing) "Tutup HUD" else "HUD Melayang", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onCleanCacheQuick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("clean_cache_quick_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = "Bersihkan Cache",
                    tint = SignalGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Bersihkan Cache", fontSize = 12.sp, color = TextPrimary)
            }
        }

        AnimatedVisibility(visible = lastCleanedMb != null) {
            lastCleanedMb?.let { mb ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ Berhasil membersihkan ${String.format("%.1f", mb)} MB memori cache!",
                    fontSize = 12.sp,
                    color = SignalGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNavigateToSpeedTest,
            colors = ButtonDefaults.buttonColors(
                containerColor = SignalGreen,
                contentColor = DeepNavy
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("speed_test_quick_button")
        ) {
            Icon(
                imageVector = Icons.Default.NetworkCheck,
                contentDescription = "Test Speed Jaringan",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Uji Kecepatan Jaringan (Speed Test)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
