package com.android.fluxcut
import java.util.Calendar
import android.content.Context
import android.content.SharedPreferences

object UserPreferences {
    private const val PREFS_NAME = "fluxcut_profile_data"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveProfile(context: Context, name: String, handle: String, bio: String, photoUri: String) {
        getPrefs(context).edit().apply {
            putString("USER_NAME", name)
            putString("USER_HANDLE", handle)
            putString("USER_BIO", bio)
            putString("USER_PHOTO_URI", photoUri)
            apply()
        }
    }

    data class UserProfile(val name: String, val handle: String, val bio: String, val photoUri: String)

    fun getProfile(context: Context): UserProfile {
        val prefs = getPrefs(context)
        return UserProfile(
            name = prefs.getString("USER_NAME", "Sathiya") ?: "Sathiya",
            handle = prefs.getString("USER_HANDLE", "@admin") ?: "@admin",
            bio = prefs.getString("USER_BIO", "Offline editor workspace.") ?: "Offline editor workspace.",
            photoUri = prefs.getString("USER_PHOTO_URI", "") ?: ""
        )
    }

    fun incrementExportCount(context: Context) {
        val prefs = getPrefs(context)
        val current = prefs.getInt("TOTAL_EXPORTS", 0)
        prefs.edit().putInt("TOTAL_EXPORTS", current + 1).apply()
    }

    fun getExportCount(context: Context): Int {
        return getPrefs(context).getInt("TOTAL_EXPORTS", 0)
    }

    fun saveSetting(context: Context, key: String, value: String) {
        getPrefs(context).edit().putString(key, value).apply()
    }

    fun getSetting(context: Context, key: String, default: String): String {
        return getPrefs(context).getString(key, default) ?: default
    }

    fun calculateWeeklyActivity(projects: List<Project>): List<Float> {
        val activityCounts = IntArray(7) { 0 }
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val todayStart = calendar.timeInMillis
        val msPerDay = 86400000L

        for (project in projects) {
            val diffDays = ((todayStart - project.lastModified) / msPerDay).toInt()
            val index = 6 - diffDays
            if (index in 0..6) {
                activityCounts[index]++
            }
        }

        val maxActivity = activityCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
        return activityCounts.map { count ->
            (count.toFloat() / maxActivity.toFloat()).coerceIn(0f, 1f)
        }
    }
}