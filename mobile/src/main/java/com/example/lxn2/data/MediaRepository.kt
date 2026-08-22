package com.example.lxn2.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

class MediaRepository(private val context: Context) {

    fun getAllMusic(): List<MediaItem> {
        return getLocalMusic() + getOnlineMusic()
    }

    private fun getLocalMusic(): List<MediaItem> {
        val musicList = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                musicList.add(
                    MediaItem.Builder()
                        .setMediaId(id.toString())
                        .setUri(contentUri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(title)
                                .setArtist(artist)
                                .build()
                        )
                        .build()
                )
            }
        }
        return musicList
    }

    private fun getOnlineMusic(): List<MediaItem> {
        return listOf(
            MediaItem.Builder()
                .setMediaId("online1")
                .setUri("https://storage.googleapis.com/exoplayer-test-media-0/play.mp3")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Sample Online Song")
                        .setArtist("ExoPlayer")
                        .build()
                )
                .build()
        )
    }
}
