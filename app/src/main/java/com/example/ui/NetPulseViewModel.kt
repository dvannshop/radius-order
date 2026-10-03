package com.example.ui

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NetPulseApp
import com.example.data.db.SpeedTestRecord
import com.example.engine.CacheBreakdown
import com.example.engine.CacheCleanerManager
import com.example.engine.PingOptimizerEngine
import com.example.engine.SpeedTestEngine
import com.example.engine.SpeedTestPhase
import com.example.model.BatteryTelemetry
import com.example.model.GpsTelemetry
import com.example.model.NetworkTelemetry
import com.example.model.PowerProfile
import com.example.service.FloatingHudService
import com.example.service.PulseForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NetPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NetPulseApp
    private val speedTestRepo = app.speedTestRepository
    private val speedTestEngine = SpeedTestEngine()
    private val cacheManager = CacheCleanerManager(application)
    private val pingOptimizer = PingOptimizerEngine()

    val isServiceRunning: StateFlow<Boolean> = PulseForegroundService.isRunning
    val networkTelemetry: StateFlow<NetworkTelemetry> = PulseForegroundService.networkTelemetry
    val gpsTelemetry: StateFlow<GpsTelemetry> = PulseForegroundService.gpsTelemetry
    val batteryTelemetry: StateFlow<BatteryTelemetry> = PulseForegroundService.batteryTelemetry
    val currentPowerProfile: StateFlow<PowerProfile> = PulseForegroundService.currentPowerProfile
    val isHudShowing: StateFlow<Boolean> = FloatingHudService.isShowing

    val speedTestProgress = speedTestEngine.progress
    val speedTestHistory: StateFlow<List<SpeedTestRecord>> = speedTestRepo.allRecords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _cacheBreakdown = MutableStateFlow(CacheBreakdown())
    val cacheBreakdown: StateFlow<CacheBreakdown> = _cacheBreakdown.asStateFlow()

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    private val _lastCleanedMb = MutableStateFlow<Double?>(null)
    val lastCleanedMb: StateFlow<Double?> = _lastCleanedMb.asStateFlow()

    private val _isOptimizingRoute = MutableStateFlow(false)
    val isOptimizingRoute: StateFlow<Boolean> = _isOptimizingRoute.asStateFlow()

    private val _routeOptimizationResult = MutableStateFlow<String?>(null)
    val routeOptimizationResult: StateFlow<String?> = _routeOptimizationResult.asStateFlow()

    init {
        refreshCacheBreakdown()
    }

    fun toggleMasterOptimizer() {
        val currentContext = getApplication<Application>()
        if (isServiceRunning.value) {
            PulseForegroundService.stopService(currentContext)
        } else {
            PulseForegroundService.startService(currentContext, currentPowerProfile.value)
        }
    }

    fun selectPowerProfile(profile: PowerProfile) {
        val currentContext = getApplication<Application>()
        PulseForegroundService.updatePowerProfile(currentContext, profile)
    }

    fun toggleFloatingHud() {
        val context = getApplication<Application>()
        if (isHudShowing.value) {
            FloatingHudService.stop(context)
        } else {
            if (Settings.canDrawOverlays(context)) {
                FloatingHudService.start(context)
            }
        }
    }

    fun runSpeedTest() {
        if (speedTestProgress.value.isRunning) return
        viewModelScope.launch {
            val result = speedTestEngine.runSpeedTest()
            if (result.phase == SpeedTestPhase.FINISHED) {
                val record = SpeedTestRecord(
                    downloadMbps = result.downloadFinalMbps,
                    uploadMbps = result.uploadFinalMbps,
                    pingMs = result.pingMs,
                    jitterMs = result.jitterMs,
                    networkType = networkTelemetry.value.networkType,
                    serverName = "Cloudflare Global CDN"
                )
                speedTestRepo.saveRecord(record)
            }
        }
    }

    fun resetSpeedTest() {
        speedTestEngine.reset()
    }

    fun clearSpeedTestHistory() {
        viewModelScope.launch {
            speedTestRepo.clearHistory()
        }
    }

    fun refreshCacheBreakdown() {
        viewModelScope.launch {
            _cacheBreakdown.value = cacheManager.calculateCacheSize()
        }
    }

    fun cleanCache() {
        if (_isCleaning.value) return
        viewModelScope.launch {
            _isCleaning.value = true
            val freed = cacheManager.clearAllCaches()
            _lastCleanedMb.value = freed
            _cacheBreakdown.value = cacheManager.calculateCacheSize()
            _isCleaning.value = false
        }
    }

    fun optimizePingRoute() {
        if (_isOptimizingRoute.value) return
        viewModelScope.launch {
            _isOptimizingRoute.value = true
            _routeOptimizationResult.value = null
            val bestCandidate = pingOptimizer.optimizeAndFindFastestRoute()
            _routeOptimizationResult.value = "Rute Terpilih: ${bestCandidate.name} (${bestCandidate.host})"
            _isOptimizingRoute.value = false
        }
    }
}
