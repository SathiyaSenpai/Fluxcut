package com.android.fluxcut

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Process
import android.os.storage.StorageManager
import java.io.File
import java.util.Locale

object CacheManager {

    fun getCacheSize(context: Context): String {
        return try {
            val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val uuid = StorageManager.UUID_DEFAULT
            val stats = storageStatsManager.queryStatsForPackage(uuid, context.packageName, Process.myUserHandle())
            formatSize(stats.cacheBytes)
        } catch (e: Exception) {
            // Fallback to manual calculation if StorageStatsManager fails
            formatSize(calculateCacheBytes(context))
        }
    }

    /**
     * Computes cache size by walking the cache directories directly instead of
     * going through StorageStatsManager. StorageStatsManager reads from the OS's
     * quota-tracking layer, which can report stale (pre-deletion) numbers for a
     * short window right after files are deleted. Use this right after
     * [clearAllCache] so the UI reflects the real, current on-disk state instead
     * of a stale quota snapshot.
     */
    fun getCacheSizeNow(context: Context): String = formatSize(calculateCacheBytes(context))

    private fun calculateCacheBytes(context: Context): Long {
        var size: Long = 0
        size += getFolderSize(context.cacheDir)
        size += getFolderSize(context.codeCacheDir)
        context.externalCacheDirs.forEach { dir ->
            if (dir != null) size += getFolderSize(dir)
        }
        return size
    }

    fun clearAllCache(context: Context): Boolean {
        return try {
            val internalCleared = deleteDirContent(context.cacheDir)
            val codeCacheCleared = deleteDirContent(context.codeCacheDir)
            var externalCleared = true
            context.externalCacheDirs.forEach { dir ->
                if (dir != null) {
                    if (!deleteDirContent(dir)) externalCleared = false
                }
            }
            internalCleared && codeCacheCleared && externalCleared
        } catch (e: Exception) {
            false
        }
    }

    private fun getFolderSize(dir: File): Long {
        var size: Long = 0
        val files = dir.listFiles() ?: return 0
        for (file in files) {
            size += if (file.isDirectory) getFolderSize(file) else file.length()
        }
        return size
    }

    private fun deleteDirContent(dir: File): Boolean {
        val files = dir.listFiles() ?: return true
        var success = true
        for (file in files) {
            val deleted = if (file.isDirectory) {
                deleteDirContent(file) && file.delete()
            } else {
                file.delete()
            }
            if (!deleted) success = false
        }
        return success
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0.0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val clampedGroup = digitGroups.coerceAtMost(units.size - 1)
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, clampedGroup.toDouble()), units[clampedGroup])
    }
}