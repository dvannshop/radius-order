package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CacheBreakdown
import com.example.model.BatteryTelemetry
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
fun ToolsScreen(
    cacheBreakdown: CacheBreakdown,
    isCleaning: Boolean,
    lastCleanedMb: Double?,
    batteryTelemetry: BatteryTelemetry,
    isHudShowing: Boolean,
    onCleanCache: () -> Unit,
    onToggleHud: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val canDrawOverlays = Settings.canDrawOverlays(context)
    val isIgnoringBattery = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
        ?.isIgnoringBatteryOptimizations(context.packageName) ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("tools_screen")
    ) {
        Text(
            text = "Alat Sistem & Pembersih",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Text(
            text = "Pembersihan cache, manajemen baterai, floating HUD & perizinan",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Cache Cleaner Section
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
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Pembersih Cache",
                            tint = SignalGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pembersih Cache Latar Belakang",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "${String.format("%.1f", cacheBreakdown.totalCacheMb)} MB",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SignalGreen
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Membersihkan file temporer pengujian jaringan, sampah soket, dan buffer yang tersimpan.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CacheDetailItem(label = "Internal Cache", value = "${String.format("%.1f", cacheBreakdown.internalCacheMb)} MB")
                    CacheDetailItem(label = "External Cache", value = "${String.format("%.1f", cacheBreakdown.externalCacheMb)} MB")
                    CacheDetailItem(label = "Total File", value = "${cacheBreakdown.totalFilesCount}")
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onCleanCache,
                    enabled = !isCleaning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalGreen,
                        contentColor = DeepNavy
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("clean_cache_button")
                ) {
                    if (isCleaning) {
                        CircularProgressIndicator(
                            color = DeepNavy,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Sedang Membersihkan...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Bersihkan",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Bersihkan Cache Sekarang", fontWeight = FontWeight.Bold)
                    }
                }

                AnimatedVisibility(visible = lastCleanedMb != null) {
                    lastCleanedMb?.let { freed ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Berhasil membebaskan ${String.format("%.1f", freed)} MB memori & RAM!",
                            fontSize = 12.sp,
                            color = SignalGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Floating Window (SYSTEM_ALERT_WINDOW)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Floating HUD",
                        tint = SignalAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Floating HUD (Tampilkan di Atas Aplikasi Lain)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Menampilkan widget mengambang kecil berisi Ping (ms), GPS (±m), dan Kecepatan (km/h) di atas aplikasi Maps atau pengantaran pesanan.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!canDrawOverlays) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Izin", tint = SignalAmber)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Beri Izin Tampilkan di Atas Aplikasi", fontSize = 12.sp, color = TextPrimary)
                    }
                } else {
                    Button(
                        onClick = onToggleHud,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHudShowing) SignalAmber else CardSurfaceElevated,
                            contentColor = if (isHudShowing) DeepNavy else TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isHudShowing) "Tutup Widget HUD Melayang" else "Tampilkan Widget HUD Melayang",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Battery Optimizer Info
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
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = "Baterai",
                            tint = if (batteryTelemetry.temperatureCelsius > 40f) SignalRed else SignalGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Status Baterai & Suhu",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "${batteryTelemetry.percentage}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CacheDetailItem(label = "Suhu Baterai", value = "${String.format("%.1f", batteryTelemetry.temperatureCelsius)}°C")
                    CacheDetailItem(label = "Kesehatan", value = batteryTelemetry.health)
                    CacheDetailItem(label = "Tegangan", value = "${batteryTelemetry.voltageMv} mV")
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isIgnoringBattery) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Izinkan Abaikan Hemat Baterai (Agar Tidak Dimatikan OS)",
                            fontSize = 11.sp,
                            color = SignalAmber
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Done, contentDescription = "Diizinkan", tint = SignalGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Bebas Pembatasan Baterai Latar Belakang (Aktif)", fontSize = 12.sp, color = SignalGreen)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Permissions Center
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Perizinan",
                        tint = SignalCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pusat Izin Akses Sistem",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                PermissionRowItem(name = "Izin Lokasi Presisi (GPS)", isGranted = true)
                PermissionRowItem(name = "Layanan Latar Belakang (Foreground)", isGranted = true)
                PermissionRowItem(name = "Notifikasi Status Terus Menerus", isGranted = true)
                PermissionRowItem(name = "Tampilkan di Atas Aplikasi (Floating HUD)", isGranted = canDrawOverlays)
                PermissionRowItem(name = "Abaikan Optimasi Baterai OS", isGranted = isIgnoringBattery)

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardSurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Cek & Perbarui Semua Izin", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CacheDetailItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun PermissionRowItem(name: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontSize = 12.sp, color = TextPrimary)
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isGranted) SignalGreen.copy(alpha = 0.15f) else SignalRed.copy(alpha = 0.15f))
                .border(1.dp, if (isGranted) SignalGreen else SignalRed, CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isGranted) "Aktif" else "Belum",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) SignalGreen else SignalRed
            )
        }
    }
}
