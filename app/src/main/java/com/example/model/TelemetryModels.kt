package com.example.model

data class NetworkTelemetry(
    val isConnected: Boolean = false,
    val networkType: String = "Memeriksa...", // "Wi-Fi", "5G", "4G LTE", "Tidak Ada Sinyal"
    val carrierOrSsid: String = "-",
    val ipAddress: String = "-",
    val currentPingMs: Long = 0L,
    val avgPingMs: Long = 0L,
    val jitterMs: Long = 0L,
    val packetLossPercent: Int = 0,
    val downloadSpeedKbps: Long = 0L,
    val uploadSpeedKbps: Long = 0L,
    val totalMobileDataMb: Double = 0.0,
    val totalWifiDataMb: Double = 0.0,
    val targetServer: String = "Google DNS (8.8.8.8)",
    val pingHistory: List<Long> = emptyList()
)

data class GpsTelemetry(
    val hasLocation: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Float = 0.0f,
    val speedKmh: Float = 0.0f,
    val altitudeMeters: Double = 0.0,
    val bearingDegrees: Float = 0.0f,
    val provider: String = "GPS",
    val satellitesUsed: Int = 0,
    val isLockedContinuous: Boolean = false,
    val lastUpdateTime: Long = 0L
) {
    val accuracyQuality: String
        get() = when {
            !hasLocation -> "Mencari Sinyal GPS..."
            accuracyMeters <= 3.0f -> "Akurasi Sempurna (±${String.format("%.1f", accuracyMeters)}m)"
            accuracyMeters <= 8.0f -> "Akurasi Baik (±${String.format("%.1f", accuracyMeters)}m)"
            accuracyMeters <= 15.0f -> "Akurasi Sedang (±${String.format("%.1f", accuracyMeters)}m)"
            else -> "Sinyal Lemah (±${String.format("%.1f", accuracyMeters)}m)"
        }
}

data class BatteryTelemetry(
    val percentage: Int = 100,
    val temperatureCelsius: Float = 30.0f,
    val voltageMv: Int = 4000,
    val health: String = "Baik",
    val isCharging: Boolean = false,
    val powerProfile: PowerProfile = PowerProfile.BALANCED
)

enum class PowerProfile(
    val title: String,
    val subtitle: String,
    val pingIntervalMs: Long,
    val gpsIntervalMs: Long
) {
    MAX_PERFORMANCE(
        title = "Performa Ekstrem (Gacor)",
        subtitle = "Ping 1s & GPS aktif konstan tanpa tidur",
        pingIntervalMs = 1000L,
        gpsIntervalMs = 1000L
    ),
    BALANCED(
        title = "Mode Seimbang (Driver)",
        subtitle = "Optimal untuk navigasi & sinyal stabil",
        pingIntervalMs = 3000L,
        gpsIntervalMs = 2500L
    ),
    BATTERY_SAVER(
        title = "Ultra Hemat Baterai",
        subtitle = "Hemat daya cerdas, interval ping 8s",
        pingIntervalMs = 8000L,
        gpsIntervalMs = 6000L
    )
}
