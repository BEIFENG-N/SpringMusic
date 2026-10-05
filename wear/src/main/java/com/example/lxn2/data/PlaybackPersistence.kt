package com.example.lxn2.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import java.io.File

class PlaybackPersistence(private val appContext: Context) {
    private val prefs: SharedPreferences = appContext.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)

    @Volatile private var cachedLastPosition: Long? = null
    @Volatile private var cachedLastIndex: Int? = null
    private val handler = Handler(Looper.getMainLooper())

    private val flushPositionRunnable = Runnable {
        cachedLastPosition?.let { pos ->
            prefs.edit { putLong("last_position", pos) }
        }
    }

    private val flushIndexRunnable = Runnable {
        cachedLastIndex?.let { index ->
            prefs.edit { putInt("last_index", index) }
        }
    }

    fun saveLastPath(path: String?) {
        prefs.edit { putString("last_path", path) }
    }

    fun getLastPath(): String? = prefs.getString("last_path", null)

    fun saveLastPlayedId(mediaId: String?) {
        prefs.edit { putString("last_played_id", mediaId) }
    }

    fun getLastPlayedId(): String? = prefs.getString("last_played_id", null)

    /**
     * History for playlist top-sorting.
     */
    fun recordPlayedMedia(mediaId: String) {
        val updated = normalizeHistory(getPlaybackHistory(), mediaId)
        prefs.edit { putString("playback_history", updated.joinToString("|")) }
        saveLastPlayedId(mediaId)
    }

    fun getPlaybackHistory(): List<String> {
        val raw = prefs.getString("playback_history", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split("|")
    }

    fun saveLastMediaIndex(index: Int) {
        cachedLastIndex = index
        handler.removeCallbacks(flushIndexRunnable)
        handler.postDelayed(flushIndexRunnable, 1000L)
    }

    fun getLastMediaIndex(): Int = cachedLastIndex ?: prefs.getInt("last_index", 0)

    fun saveLastPosition(position: Long) {
        cachedLastPosition = position
        handler.removeCallbacks(flushPositionRunnable)
        handler.postDelayed(flushPositionRunnable, 1000L)
    }

    fun getLastPosition(): Long = cachedLastPosition ?: prefs.getLong("last_position", 0L)

    fun flushPendingWrites() {
        handler.removeCallbacks(flushPositionRunnable)
        handler.removeCallbacks(flushIndexRunnable)
        prefs.edit(commit = true) {
            cachedLastPosition?.let { putLong("last_position", it) }
            cachedLastIndex?.let { putInt("last_index", it) }
        }
    }

    fun saveActiveSourcePath(path: String?) {
        prefs.edit { putString("active_source_path", path) }
    }

    fun getActiveSourcePath(): String? = prefs.getString("active_source_path", null)

    fun saveBackgroundBlur(enabled: Boolean) {
        prefs.edit { putBoolean("background_blur", enabled) }
    }

    fun isBackgroundBlurEnabled(): Boolean = prefs.getBoolean("background_blur", true)

    fun saveDisplayEffect(effect: String) {
        prefs.edit { putString("display_effect", effect) }
    }

    fun getDisplayEffect(): String = prefs.getString("display_effect", "GLASSMORPHISM") ?: "GLASSMORPHISM"

    fun saveAppScale(scale: Float) {
        prefs.edit { putFloat("app_scale", scale) }
    }

    fun getAppScale(): Float = prefs.getFloat("app_scale", 1.0f)

    fun saveThemeColor(color: Long) {
        prefs.edit { putLong("theme_color", color) }
    }

    fun getThemeColor(): Long = prefs.getLong("theme_color", 0xFFB7E4C7)

    fun markPermissionPromptShown() {
        prefs.edit { putBoolean("permission_prompt_shown", true) }
    }

    fun isPermissionPromptShown(): Boolean = prefs.getBoolean("permission_prompt_shown", false)

    // Session Playlist Persistence
    fun saveSessionPlaylist(json: String) {
        try {
            File(appContext.filesDir, MediaCacheCodec.SESSION_FILE).writeText(json)
        } catch (e: Exception) {
            Log.e(AppConstants.LOG_TAG, "Error saving session playlist", e)
        }
    }

    fun getSessionPlaylist(): String? {
        val file = File(appContext.filesDir, MediaCacheCodec.SESSION_FILE)
        return if (file.exists()) file.readText() else null
    }

    // --- Dislike Persistence ---
    fun saveDislikeKeys(keys: Set<String>) {
        prefs.edit { putStringSet("dislike_keys", keys) }
    }

    fun getDislikeKeys(): Set<String> {
        return prefs.getStringSet("dislike_keys", emptySet()) ?: emptySet()
    }

    // --- Favorite / Love Playlist Persistence ---
    fun saveFavoriteIds(ids: Set<String>) {
        prefs.edit { putStringSet("favorite_ids", ids) }
    }

    fun getFavoriteIds(): Set<String> {
        return prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(mediaId: String): Boolean {
        val favorites = getFavoriteIds().toMutableSet()
        val isFavorite = if (favorites.contains(mediaId)) {
            favorites.remove(mediaId)
            false
        } else {
            favorites.add(mediaId)
            true
        }
        saveFavoriteIds(favorites)
        return isFavorite
    }

    fun isFavorite(mediaId: String): Boolean {
        return getFavoriteIds().contains(mediaId)
    }

    // --- App & Player Settings Persistence ---
    fun savePlaybackRate(rate: Float) {
        prefs.edit { putFloat("playback_rate", rate) }
    }

    fun getPlaybackRate(): Float = prefs.getFloat("playback_rate", 1.0f)

    fun saveShowLyricTranslation(enabled: Boolean) {
        prefs.edit { putBoolean("show_lyric_translation", enabled) }
    }

    fun isShowLyricTranslation(): Boolean = prefs.getBoolean("show_lyric_translation", false)

    fun saveShowLyricRoma(enabled: Boolean) {
        prefs.edit { putBoolean("show_lyric_roma", enabled) }
    }

    fun isShowLyricRoma(): Boolean = prefs.getBoolean("show_lyric_roma", false)

    fun saveAutoCleanPlayedList(enabled: Boolean) {
        prefs.edit { putBoolean("auto_clean_played_list", enabled) }
    }

    fun isAutoCleanPlayedList(): Boolean = prefs.getBoolean("auto_clean_played_list", false)

    fun saveAddMusicLocationTop(top: Boolean) {
        prefs.edit { putBoolean("add_music_location_top", top) }
    }

    fun isAddMusicLocationTop(): Boolean = prefs.getBoolean("add_music_location_top", true)

    // --- Search History Persistence ---
    fun saveSearchHistory(history: List<String>) {
        prefs.edit { putString("search_history", history.joinToString("|")) }
    }

    fun getSearchHistory(): List<String> {
        val raw = prefs.getString("search_history", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split("|").filter { it.isNotBlank() }
    }

    fun addSearchHistory(keyword: String) {
        if (keyword.isBlank()) return
        val current = getSearchHistory().filter { it != keyword }.toMutableList()
        current.add(0, keyword)
        saveSearchHistory(if (current.size > 20) current.take(20) else current)
    }

    fun clearSearchHistory() {
        prefs.edit { remove("search_history") }
    }

    fun getLastPlayedIdFromZero(): String? = getLastPlayedId()

    companion object {
        const val MAX_HISTORY = 100

        fun normalizeHistory(current: List<String>, newMediaId: String): List<String> {
            val history = current
                .filter { it.isNotBlank() && it != newMediaId }
                .toMutableList()
            history.add(0, newMediaId)
            return if (history.size > MAX_HISTORY) history.take(MAX_HISTORY) else history
        }
    }
}
