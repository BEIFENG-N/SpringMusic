package com.example.lxn2.data

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject

/**
 * 媒体索引缓存的编解码器。
 */
object MediaCacheCodec {

    const val CACHE_FILE = "music_cache_v6.txt"
    const val SESSION_FILE = "session_playlist.txt"

    fun decode(content: String): List<MediaItem> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()
        return if (trimmed.startsWith("[")) decodeJson(trimmed) else decodePipe(trimmed)
    }

    fun encode(items: List<MediaItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val meta = item.mediaMetadata
            val extras = meta.extras
            val filePath = extras?.getString("file_path") ?: ""
            val origin = extras?.getString("origin") ?: "LOCAL"
            val source = extras?.getString("source") ?: ""
            
            val row = JSONObject().apply {
                put("id", item.mediaId)
                put("uri", item.localConfiguration?.uri?.toString() ?: "")
                put("title", meta.title?.toString() ?: "")
                put("artist", meta.artist?.toString() ?: "")
                put("album", meta.albumTitle?.toString() ?: "")
                put("artworkUri", meta.artworkUri?.toString() ?: "")
                put("filePath", filePath)
                put("origin", origin)
                put("source", source)
            }
            arr.put(row)
        }
        return arr.toString()
    }

    private fun decodeJson(content: String): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val arr = JSONArray(content)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            result.add(
                buildItem(
                    id = obj.optString("id"),
                    uri = obj.optString("uri"),
                    title = obj.optString("title"),
                    artist = obj.optString("artist"),
                    album = obj.optString("album"),
                    filePath = obj.optString("filePath"),
                    artworkUri = obj.optString("artworkUri").takeIf { it.isNotBlank() },
                    origin = obj.optString("origin", "LOCAL"),
                    source = obj.optString("source", "")
                )
            )
        }
        return result
    }

    private fun decodePipe(content: String): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        content.lines().forEach { line ->
            if (line.isBlank()) return@forEach
            val parts = line.split("|")
            if (parts.size < 5) return@forEach
            result.add(
                buildItem(
                    id = parts[0],
                    uri = Uri.fromFile(java.io.File(parts[4])).toString(),
                    title = parts[1],
                    artist = parts[2],
                    album = parts[3],
                    filePath = parts[4],
                    artworkUri = parts.getOrNull(5)?.takeIf { it.isNotBlank() },
                    origin = "LOCAL",
                    source = ""
                )
            )
        }
        return result
    }

    private fun buildItem(
        id: String,
        uri: String,
        title: String,
        artist: String,
        album: String,
        filePath: String,
        artworkUri: String?,
        origin: String,
        source: String
    ): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title.ifBlank { "Unknown" })
            .setArtist(artist.ifBlank { "Unknown" })
            .setAlbumTitle(album.ifBlank { "Unknown" })

        artworkUri?.let { metadataBuilder.setArtworkUri(Uri.parse(it)) }

        val bundle = Bundle().apply {
            putString("origin", origin)
            if (filePath.isNotBlank()) putString("file_path", filePath)
            if (source.isNotBlank()) putString("source", source)
        }
        metadataBuilder.setExtras(bundle)

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(Uri.parse(uri))
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }
}
