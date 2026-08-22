package com.example.lxn2.data

import android.content.Context
import android.content.SharedPreferences

import androidx.core.content.edit

class PlaybackPersistence(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)

    fun saveLastPath(path: String?) {
        prefs.edit { putString("last_path", path) }
    }

    fun getLastPath(): String? = prefs.getString("last_path", null)

    /**
     * Records the last clicked song's ID as marker "0".
     */
    fun saveLastPlayedIdAsZero(mediaId: String?) {
        prefs.edit { putString("0", mediaId) }
    }

    fun getLastPlayedIdFromZero(): String? = prefs.getString("0", null)

    /**
     * History for playlist top-sorting.
     */
    fun recordPlayedMedia(mediaId: String) {
        val history = getPlaybackHistory().toMutableList()
        history.remove(mediaId)
        history.add(0, mediaId)
        val limitedHistory = if (history.size > 100) history.take(100) else history
        prefs.edit { putString("playback_history", limitedHistory.joinToString("|")) }
        
        // Also strictly save as the "ID 0" last clicked marker
        saveLastPlayedIdAsZero(mediaId)
    }

    fun getPlaybackHistory(): List<String> {
        val raw = prefs.getString("playback_history", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split("|")
    }

    fun saveLastMediaIndex(index: Int) {
        prefs.edit { putInt("last_index", index) }
    }

    fun getLastMediaIndex(): Int = prefs.getInt("last_index", 0)

    fun saveActiveSourcePath(path: String?) {
        prefs.edit { putString("active_source_path", path) }
    }

    fun getActiveSourcePath(): String? = prefs.getString("active_source_path", null)

    fun saveBackgroundBlur(enabled: Boolean) {
        prefs.edit { putBoolean("background_blur", enabled) }
    }

    fun isBackgroundBlurEnabled(): Boolean = prefs.getBoolean("background_blur", true)

    fun saveThemeColor(color: Long) {
        prefs.edit { putLong("theme_color", color) }
    }

    fun getThemeColor(): Long = prefs.getLong("theme_color", 0xFFB7E4C7) // 默认香草绿
}
