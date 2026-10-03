package com.example.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkStatsManager(private val context: Context) {

    private var lastRxBytes: Long = TrafficStats.getTotalRxBytes()
    private var lastTxBytes: Long = TrafficStats.getTotalTxBytes()
    private var lastCheckTime: Long = System.currentTimeMillis()

    fun getInstantThroughput(): Pair<Long, Long> {
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()

        val timeDiffSec = ((currentTime - lastCheckTime).coerceAtLeast(100L)) / 1000.0

        val rxDiff = if (lastRxBytes > 0 && currentRx >= lastRxBytes) currentRx - lastRxBytes else 0L
        val txDiff = if (lastTxBytes > 0 && currentTx >= lastTxBytes) currentTx - lastTxBytes else 0L

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastCheckTime = currentTime

        val downloadKbps = (rxDiff / 1024.0 / timeDiffSec).toLong().coerceAtLeast(0L)
        val uploadKbps = (txDiff / 1024.0 / timeDiffSec).toLong().coerceAtLeast(0L)

        return Pair(downloadKbps, uploadKbps)
    }

    fun getTotalMobileDataMb(): Double {
        val bytes = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()
        return if (bytes > 0) (bytes / (1024.0 * 1024.0)) else 0.0
    }

    fun getTotalWifiDataMb(): Double {
        val total = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()
        val mobile = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()
        val wifi = (total - mobile).coerceAtLeast(0L)
        return (wifi / (1024.0 * 1024.0))
    }

    fun getNetworkInfo(): Triple<Boolean, String, String> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return Triple(false, "Tidak Ada Jaringan", "-")

        val activeNetwork = cm.activeNetwork ?: return Triple(false, "Terputus", "-")
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return Triple(false, "Terputus", "-")

        val isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val ssid = wifiManager?.connectionInfo?.ssid?.replace("\"", "") ?: "Wi-Fi Aktif"
                Triple(isConnected, "Wi-Fi", if (ssid == "<unknown ssid>") "Wi-Fi Terkoneksi" else ssid)
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                val operatorName = telephonyManager?.networkOperatorName?.ifEmpty { "Seluler" } ?: "Seluler"
                val gen = getCellularGeneration(caps)
                Triple(isConnected, gen, operatorName)
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                Triple(isConnected, "Ethernet", "LAN")
            }
            else -> Triple(isConnected, "Jaringan Lain", "-")
        }
    }

    private fun getCellularGeneration(caps: NetworkCapabilities): String {
        return if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
            "5G / LTE Cepat"
        } else {
            "4G LTE / 5G"
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "-"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }
}
