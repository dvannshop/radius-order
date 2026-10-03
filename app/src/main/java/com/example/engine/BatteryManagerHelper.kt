package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import com.example.model.BatteryTelemetry
import com.example.model.PowerProfile

class BatteryManagerHelper(private val context: Context) {

    fun getBatteryTelemetry(currentProfile: PowerProfile): BatteryTelemetry {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

        if (batteryStatus == null) {
            return BatteryTelemetry(powerProfile = currentProfile)
        }

        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level != -1 && scale != -1) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 100

        val tempRaw = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300)
        val tempC = tempRaw / 10.0f

        val voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000)

        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val healthRaw = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
        val health = when (healthRaw) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Baik (Sehat)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Terlalu Panas!"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Rusak"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Tegangan Berlebih"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Gagal"
            else -> "Normal"
        }

        return BatteryTelemetry(
            percentage = pct,
            temperatureCelsius = tempC,
            voltageMv = voltage,
            health = health,
            isCharging = isCharging,
            powerProfile = currentProfile
        )
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    @SuppressLint("BatteryLife")
    fun createIgnoreBatteryOptimizationsIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
