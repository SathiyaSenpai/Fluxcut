package com.android.fluxcut

import android.content.Context
import java.io.File

object CacheManager {

    fun getCacheSize(context: Context): String {
        var size: Long = 0
        size += getFolderSize(context.cacheDir)
        context.externalCacheDir?.let {
            size += getFolderSize(it)
        }
        return formatSize(size)
    }

    fun clearAllCache(context: Context): Boolean {
        return try {
            val internalCleared = deleteDirContent(context.cacheDir)
            val externalCleared = context.externalCacheDir?.let { deleteDirContent(it) } ?: true
            internalCleared && externalCleared
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
        if (size <= 0) return "0 KB"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val clampedGroup = digitGroups.coerceAtMost(units.size - 1)
        return String.format("%.1f %s", size / Math.pow(1024.0, clampedGroup.toDouble()), units[clampedGroup])
    }
}
