package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs

data class PingResult(
    val latencyMs: Long,
    val isSuccess: Boolean,
    val target: String
)

data class DnsCandidate(
    val name: String,
    val host: String,
    val port: Int = 53
)

class PingOptimizerEngine {

    val targetCandidates = listOf(
        DnsCandidate("Cloudflare DNS", "1.1.1.1", 53),
        DnsCandidate("Google Public DNS", "8.8.8.8", 53),
        DnsCandidate("OpenDNS", "208.67.222.222", 53),
        DnsCandidate("Quad9 Security DNS", "9.9.9.9", 53)
    )

    private val recentPings = mutableListOf<Long>()
    private var failedCount = 0
    private var totalCount = 0

    suspend fun executePing(host: String = "8.8.8.8", port: Int = 53, timeoutMs: Int = 1800): PingResult =
        withContext(Dispatchers.IO) {
            val startTime = System.nanoTime()
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val durationMs = ((System.nanoTime() - startTime) / 1_000_000).coerceAtLeast(1L)
                recordPing(durationMs, true)
                PingResult(latencyMs = durationMs, isSuccess = true, target = host)
            } catch (e: Exception) {
                // Secondary fallback probe via HTTP 80/443 or Google 204
                val fallbackStart = System.nanoTime()
                var fallbackSuccess = false
                var fallbackMs = 999L

                try {
                    val fbSocket = Socket()
                    fbSocket.tcpNoDelay = true
                    val fbPort = if (host == "1.1.1.1" || host == "8.8.8.8") 443 else 80
                    fbSocket.connect(InetSocketAddress(host, fbPort), timeoutMs)
                    fbSocket.close()
                    fallbackMs = ((System.nanoTime() - fallbackStart) / 1_000_000).coerceAtLeast(1L)
                    fallbackSuccess = true
                } catch (_: Exception) {
                    try {
                        val url = java.net.URL("http://connectivitycheck.gstatic.com/generate_204")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = timeoutMs
                        conn.readTimeout = timeoutMs
                        conn.requestMethod = "HEAD"
                        val code = conn.responseCode
                        conn.disconnect()
                        if (code in 200..399) {
                            fallbackMs = ((System.nanoTime() - fallbackStart) / 1_000_000).coerceAtLeast(1L)
                            fallbackSuccess = true
                        }
                    } catch (_: Exception) {}
                }

                if (fallbackSuccess) {
                    recordPing(fallbackMs, true)
                    PingResult(latencyMs = fallbackMs, isSuccess = true, target = host)
                } else {
                    recordPing(999L, false)
                    PingResult(latencyMs = 999L, isSuccess = false, target = host)
                }
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }

    private fun recordPing(latencyMs: Long, isSuccess: Boolean) {
        totalCount++
        if (!isSuccess) {
            failedCount++
        }
        synchronized(recentPings) {
            recentPings.add(latencyMs)
            if (recentPings.size > 30) recentPings.removeAt(0)
        }
    }

    fun getPingHistory(): List<Long> {
        return synchronized(recentPings) { recentPings.toList() }
    }

    fun getAveragePing(): Long {
        val list = synchronized(recentPings) { recentPings.toList() }
        if (list.isEmpty()) return 0L
        return list.average().toLong()
    }

    fun calculateJitter(): Long {
        val list = synchronized(recentPings) { recentPings.toList() }
        if (list.size < 2) return 0L
        var totalDiff = 0L
        for (i in 1 until list.size) {
            totalDiff += abs(list[i] - list[i - 1])
        }
        return totalDiff / (list.size - 1)
    }

    fun getPacketLossPercent(): Int {
        if (totalCount == 0) return 0
        return ((failedCount.toFloat() / totalCount.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    suspend fun optimizeAndFindFastestRoute(): DnsCandidate = withContext(Dispatchers.IO) {
        var fastest = targetCandidates.first()
        var minLatency = Long.MAX_VALUE

        for (candidate in targetCandidates) {
            val res = executePing(candidate.host, candidate.port, 1500)
            if (res.isSuccess && res.latencyMs < minLatency) {
                minLatency = res.latencyMs
                fastest = candidate
            }
        }
        fastest
    }

    fun resetStats() {
        synchronized(recentPings) {
            recentPings.clear()
        }
        failedCount = 0
        totalCount = 0
    }
}
