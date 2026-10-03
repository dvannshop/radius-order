package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.PowerManager
import com.example.model.GpsTelemetry
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GpsBoosterEngine(private val context: Context) {

    private val _gpsTelemetry = MutableStateFlow(GpsTelemetry())
    val gpsTelemetry: StateFlow<GpsTelemetry> = _gpsTelemetry.asStateFlow()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var isTracking = false
    private var isContinuousLockEnabled = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                updateFromLocation(location, "Fused GPS")
            }
        }
    }

    private val legacyLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateFromLocation(location, "Native GPS")
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun updateFromLocation(location: Location, providerName: String) {
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0.0f
        val current = _gpsTelemetry.value
        _gpsTelemetry.value = current.copy(
            hasLocation = true,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else 5.0f,
            speedKmh = speedKmh,
            altitudeMeters = if (location.hasAltitude()) location.altitude else 0.0,
            bearingDegrees = if (location.hasBearing()) location.bearing else 0.0f,
            provider = providerName,
            isLockedContinuous = isContinuousLockEnabled,
            lastUpdateTime = System.currentTimeMillis()
        )
    }

    @SuppressLint("MissingPermission")
    fun startGpsUpdates(intervalMs: Long = 2000L, continuousLock: Boolean = true) {
        if (isTracking) return

        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            _gpsTelemetry.value = _gpsTelemetry.value.copy(
                hasLocation = false,
                provider = "Menunggu Izin Lokasi"
            )
            return
        }

        isTracking = true
        isContinuousLockEnabled = continuousLock

        if (continuousLock) {
            acquireWakeLock()
        }

        // Fetch last known location immediately for instant UI feedback
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null && !_gpsTelemetry.value.hasLocation) {
                    updateFromLocation(lastLoc, "Fused GPS (Cepat)")
                }
            }
        } catch (_: Exception) {}

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs / 2)
                .setMinUpdateDistanceMeters(0f)
                .setWaitForAccurateLocation(true)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (_: Exception) {
            // Fallback to LocationManager native provider
            try {
                if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        intervalMs,
                        0f,
                        legacyLocationListener,
                        Looper.getMainLooper()
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun stopGpsUpdates() {
        isTracking = false
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}
        try {
            locationManager?.removeUpdates(legacyLocationListener)
        } catch (_: Exception) {}

        releaseWakeLock()
        _gpsTelemetry.value = _gpsTelemetry.value.copy(
            isLockedContinuous = false
        )
    }

    fun setContinuousLock(enabled: Boolean) {
        isContinuousLockEnabled = enabled
        _gpsTelemetry.value = _gpsTelemetry.value.copy(isLockedContinuous = enabled)
        if (enabled && isTracking) {
            acquireWakeLock()
        } else if (!enabled) {
            releaseWakeLock()
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NetPulse:GpsWakeLock")
        }
        if (wakeLock?.isHeld == false) {
            try {
                wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24 hours max safeguard
            } catch (_: Exception) {}
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }
}
