package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.engine.BatteryManagerHelper
import com.example.engine.GpsBoosterEngine
import com.example.engine.NetworkStatsManager
import com.example.engine.PingOptimizerEngine
import com.example.model.BatteryTelemetry
import com.example.model.GpsTelemetry
import com.example.model.NetworkTelemetry
import com.example.model.PowerProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PulseForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "netpulse_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_SET_PROFILE = "com.example.service.ACTION_SET_PROFILE"
        const val EXTRA_PROFILE = "extra_power_profile"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _networkTelemetry = MutableStateFlow(NetworkTelemetry())
        val networkTelemetry: StateFlow<NetworkTelemetry> = _networkTelemetry.asStateFlow()

        private val _gpsTelemetry = MutableStateFlow(GpsTelemetry())
        val gpsTelemetry: StateFlow<GpsTelemetry> = _gpsTelemetry.asStateFlow()

        private val _batteryTelemetry = MutableStateFlow(BatteryTelemetry())
        val batteryTelemetry: StateFlow<BatteryTelemetry> = _batteryTelemetry.asStateFlow()

        private val _currentPowerProfile = MutableStateFlow(PowerProfile.BALANCED)
        val currentPowerProfile: StateFlow<PowerProfile> = _currentPowerProfile.asStateFlow()

        fun startService(context: Context, profile: PowerProfile = PowerProfile.BALANCED) {
            val intent = Intent(context, PulseForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROFILE, profile.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PulseForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updatePowerProfile(context: Context, profile: PowerProfile) {
            _currentPowerProfile.value = profile
            val intent = Intent(context, PulseForegroundService::class.java).apply {
                action = ACTION_SET_PROFILE
                putExtra(EXTRA_PROFILE, profile.name)
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var pingJob: Job? = null
    private var statsJob: Job? = null

    private lateinit var pingEngine: PingOptimizerEngine
    private lateinit var gpsEngine: GpsBoosterEngine
    private lateinit var networkStatsManager: NetworkStatsManager
    private lateinit var batteryHelper: BatteryManagerHelper

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        pingEngine = PingOptimizerEngine()
        gpsEngine = GpsBoosterEngine(this)
        networkStatsManager = NetworkStatsManager(this)
        batteryHelper = BatteryManagerHelper(this)

        serviceScope.launch {
            gpsEngine.gpsTelemetry.collect { telemetry ->
                _gpsTelemetry.value = telemetry
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val profileName = intent.getStringExtra(EXTRA_PROFILE) ?: PowerProfile.BALANCED.name
                val profile = try {
                    PowerProfile.valueOf(profileName)
                } catch (_: Exception) {
                    PowerProfile.BALANCED
                }
                _currentPowerProfile.value = profile
                startOptimizer(profile)
            }
            ACTION_STOP -> {
                stopOptimizer()
                stopSelf()
            }
            ACTION_SET_PROFILE -> {
                val profileName = intent.getStringExtra(EXTRA_PROFILE) ?: PowerProfile.BALANCED.name
                val profile = try {
                    PowerProfile.valueOf(profileName)
                } catch (_: Exception) {
                    PowerProfile.BALANCED
                }
                _currentPowerProfile.value = profile
                restartLoopsWithProfile(profile)
            }
        }
        return START_STICKY
    }

    private fun startOptimizer(profile: PowerProfile) {
        _isRunning.value = true

        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val notification = buildNotification("Menginisialisasi stabilisator sinyal & GPS...", 0L, 0f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = if (hasFine || hasCoarse) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        gpsEngine.startGpsUpdates(intervalMs = profile.gpsIntervalMs, continuousLock = true)
        startLoops(profile)
    }

    private fun restartLoopsWithProfile(profile: PowerProfile) {
        pingJob?.cancel()
        statsJob?.cancel()
        gpsEngine.stopGpsUpdates()
        gpsEngine.startGpsUpdates(intervalMs = profile.gpsIntervalMs, continuousLock = true)
        startLoops(profile)
    }

    private fun startLoops(profile: PowerProfile) {
        // Ping Keep-Alive Loop
        pingJob = serviceScope.launch(Dispatchers.IO) {
            var counter = 0
            while (isActive) {
                val candidate = pingEngine.targetCandidates.first()
                val pingRes = pingEngine.executePing(candidate.host, candidate.port, 1800)

                val avg = pingEngine.getAveragePing()
                val jitter = pingEngine.calculateJitter()
                val packetLoss = pingEngine.getPacketLossPercent()
                val history = pingEngine.getPingHistory()

                val (isConnected, type, carrier) = networkStatsManager.getNetworkInfo()
                val (dlSpeed, ulSpeed) = networkStatsManager.getInstantThroughput()
                val mobData = networkStatsManager.getTotalMobileDataMb()
                val wifiData = networkStatsManager.getTotalWifiDataMb()

                _networkTelemetry.value = NetworkTelemetry(
                    isConnected = isConnected,
                    networkType = type,
                    carrierOrSsid = carrier,
                    ipAddress = networkStatsManager.getLocalIpAddress(),
                    currentPingMs = pingRes.latencyMs,
                    avgPingMs = avg,
                    jitterMs = jitter,
                    packetLossPercent = packetLoss,
                    downloadSpeedKbps = dlSpeed,
                    uploadSpeedKbps = ulSpeed,
                    totalMobileDataMb = mobData,
                    totalWifiDataMb = wifiData,
                    targetServer = "${candidate.name} (${candidate.host})",
                    pingHistory = history
                )

                // Update notification every 3 pings to save CPU/battery
                counter++
                if (counter % 2 == 0) {
                    val gpsAcc = _gpsTelemetry.value.accuracyMeters
                    updateNotification(pingRes.latencyMs, gpsAcc, profile)
                }

                delay(profile.pingIntervalMs)
            }
        }

        // Battery Monitor Loop
        statsJob = serviceScope.launch(Dispatchers.Default) {
            while (isActive) {
                _batteryTelemetry.value = batteryHelper.getBatteryTelemetry(profile)
                delay(5000L)
            }
        }
    }

    private fun stopOptimizer() {
        _isRunning.value = false
        pingJob?.cancel()
        statsJob?.cancel()
        gpsEngine.stopGpsUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification(statusText: String, pingMs: Long, gpsAcc: Float): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PulseForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = if (pingMs > 0) {
            "⚡ Ping: ${pingMs}ms | 📍 GPS: ±${String.format("%.1f", gpsAcc)}m"
        } else {
            statusText
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NetPulse Sinyal & GPS Aktif")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .addAction(0, "Hentikan", stopPendingIntent)
            .build()
    }

    private fun updateNotification(pingMs: Long, gpsAcc: Float, profile: PowerProfile) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notification = buildNotification(
            "⚡ Ping: ${pingMs}ms | 📍 GPS: ±${String.format("%.1f", gpsAcc)}m (${profile.title})",
            pingMs,
            gpsAcc
        )
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_notification_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopOptimizer()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
