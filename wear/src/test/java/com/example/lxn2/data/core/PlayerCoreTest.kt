package com.example.lxn2.data.core

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayerCoreTest {

    private fun createMediaItem(id: String, title: String, artist: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .build()
            )
            .build()
    }

    @Before
    fun setUp() {
        PlayerCore.clearTempPlayList()
        PlayerCore.clearPlayedList()
        PlayerCore.setDislikeInfo(DislikeInfo())
        PlayerCore.setIsPlayedStop(false)
        PlayerCore.setStatusText("")
    }

    @Test
    fun testTempPlayListOperations() {
        val song1 = createMediaItem("1", "Song One", "Artist A")
        val song2 = createMediaItem("2", "Song Two", "Artist B")

        PlayerCore.addTempPlayList(listOf(song1, song2))
        assertEquals(2, PlayerCore.tempPlayList.value.size)

        val popped = PlayerCore.popTempItem()
        assertNotNull(popped)
        assertEquals("1", popped?.mediaId)
        assertEquals(1, PlayerCore.tempPlayList.value.size)

        PlayerCore.clearTempPlayList()
        assertEquals(0, PlayerCore.tempPlayList.value.size)
    }

    @Test
    fun testPlayedListOperations() {
        val song1 = createMediaItem("1", "Song One", "Artist A")
        val song2 = createMediaItem("2", "Song Two", "Artist B")

        PlayerCore.addPlayedList(song1)
        PlayerCore.addPlayedList(song2)
        assertEquals(2, PlayerCore.playedList.value.size)

        PlayerCore.removePlayedList(0)
        assertEquals(1, PlayerCore.playedList.value.size)
        assertEquals("2", PlayerCore.playedList.value[0].mediaId)

        PlayerCore.clearPlayedList()
        assertEquals(0, PlayerCore.playedList.value.size)
    }

    @Test
    fun testDislikeInfoAndFiltering() {
        val song1 = createMediaItem("1", "Happy Song", "Good Artist")
        val song2 = createMediaItem("2", "Bad Song", "Bad Artist")

        PlayerCore.addDislike("Bad Song", "Bad Artist")

        assertTrue(PlayerCore.isDisliked(song2))
        assertFalse(PlayerCore.isDisliked(song1))

        val list = listOf(song1, song2)
        val filtered = PlayerCore.filterList(list)

        assertEquals(1, filtered.size)
        assertEquals("1", filtered[0].mediaId)
    }

    @Test
    fun testTimeoutExitFlag() {
        assertFalse(PlayerCore.isPlayedStop.value)
        PlayerCore.setIsPlayedStop(true)
        assertTrue(PlayerCore.isPlayedStop.value)
    }
}
