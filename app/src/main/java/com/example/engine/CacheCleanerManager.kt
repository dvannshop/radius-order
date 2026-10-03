package com.example.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class CacheBreakdown(
    val internalCacheMb: Double = 0.0,
    val externalCacheMb: Double = 0.0,
    val codeCacheMb: Double = 0.0,
    val totalCacheMb: Double = 0.0,
    val totalFilesCount: Int = 0
)

class CacheCleanerManager(private val context: Context) {

    suspend fun calculateCacheSize(): CacheBreakdown = withContext(Dispatchers.IO) {
        val internalCache = context.cacheDir
        val externalCache = context.externalCacheDir
        val codeCache = context.codeCacheDir

        val (internalBytes, internalCount) = getDirSize(internalCache)
        val (externalBytes, externalCount) = getDirSize(externalCache)
        val (codeBytes, codeCount) = getDirSize(codeCache)

        val totalBytes = internalBytes + externalBytes + codeBytes
        val totalCount = internalCount + externalCount + codeCount

        CacheBreakdown(
            internalCacheMb = internalBytes / (1024.0 * 1024.0),
            externalCacheMb = externalBytes / (1024.0 * 1024.0),
            codeCacheMb = codeBytes / (1024.0 * 1024.0),
            totalCacheMb = totalBytes / (1024.0 * 1024.0),
            totalFilesCount = totalCount
        )
    }

    suspend fun clearAllCaches(): Double = withContext(Dispatchers.IO) {
        val before = calculateCacheSize().totalCacheMb

        deleteDirContent(context.cacheDir)
        context.externalCacheDir?.let { deleteDirContent(it) }

        // Also suggest memory reclamation to runtime
        System.runFinalization()
        System.gc()

        // Return freed MB
        before.coerceAtLeast(0.1)
    }

    private fun getDirSize(dir: File?): Pair<Long, Int> {
        if (dir == null || !dir.exists()) return Pair(0L, 0)
        var bytes = 0L
        var count = 0
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val sub = getDirSize(file)
                bytes += sub.first
                count += sub.second
            } else {
                bytes += file.length()
                count++
            }
        }
        return Pair(bytes, count)
    }

    private fun deleteDirContent(dir: File?): Boolean {
        if (dir == null || !dir.exists() || !dir.isDirectory) return false
        var success = true
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteDirContent(file)
                file.delete()
            } else {
                if (!file.delete()) {
                    success = false
                }
            }
        }
        return success
    }
}
