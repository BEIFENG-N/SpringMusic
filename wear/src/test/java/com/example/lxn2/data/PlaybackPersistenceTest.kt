package com.example.lxn2.data

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaybackPersistenceTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val persistence = PlaybackPersistence(context)

    @Test
    fun normalizeHistory_movesNewToFrontAndDedups() {
        val result = PlaybackPersistence.normalizeHistory(listOf("1", "2", "3"), "2")
        assertEquals(listOf("2", "1", "3"), result)
    }

    @Test
    fun normalizeHistory_ignoresBlankAndTruncatesToMax() {
        val huge = (1..PlaybackPersistence.MAX_HISTORY + 5).map { it.toString() }
        val result = PlaybackPersistence.normalizeHistory(huge, "new")
        assertEquals(PlaybackPersistence.MAX_HISTORY, result.size)
        assertEquals("new", result.first())
    }

    @Test
    fun recordPlayedMedia_dedupsAndPersists() {
        persistence.recordPlayedMedia("1")
        persistence.recordPlayedMedia("2")
        persistence.recordPlayedMedia("1")

        val history = persistence.getPlaybackHistory()
        assertEquals(listOf("1", "2"), history)
        // 同时也更新了 "ID 0" 最近点击标记
        assertEquals("1", persistence.getLastPlayedIdFromZero())
    }

    @Test
    fun saveAndGetLastPlayedIdFromZero() {
        persistence.saveLastPlayedId("abc")
        assertEquals("abc", persistence.getLastPlayedIdFromZero())
        persistence.saveLastPlayedId(null)
        assertNull(persistence.getLastPlayedIdFromZero())
    }

    @Test
    fun themeColorDefaultsToVanillaGreen() {
        assertEquals(0xFFB7E4C7, persistence.getThemeColor())
    }
}
