package com.example.lxn2.data

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject

/**
 * 媒体索引缓存的编解码器。
 *
 * 版本演进：
 * - v5：`id|title|artist|album|path|artworkUri`（管道分隔），保留向后兼容。
 * - v6：JSON 数组（更健壮，字段值可含特殊字符），当前写入格式。
 *
 * 说明：本对象只负责"字符串/MediaItem"之间的转换与文件内容解析，
 * 不含任何 I/O 或 Android 上下文，便于单元测试。
 */
object MediaCacheCodec {

    const val CACHE_FILE = "music_cache_v6.txt"

    /**
     * 从缓存文件内容解析为媒体列表。
     * 自动识别 v6(JSON) 与 v5(管道分隔) 两种格式；空内容返回空列表。
     */
    fun decode(content: String): List<MediaItem> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()
        return if (trimmed.startsWith("[")) decodeJson(trimmed) else decodePipe(trimmed)
    }

    /**
     * 将媒体列表序列化为 v6 JSON 字符串。
     */
    fun encode(items: List<MediaItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val meta = item.mediaMetadata
            val extras = meta.extras
            val filePath = extras?.getString("file_path")
                ?: item.localConfiguration?.uri?.path
                ?: ""
            val row = JSONObject().apply {
                put("id", item.mediaId)
                put("title", meta.title?.toString() ?: "")
                put("artist", meta.artist?.toString() ?: "")
                put("album", meta.albumTitle?.toString() ?: "")
                put("artworkUri", meta.artworkUri?.toString() ?: "")
                put("filePath", filePath)
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
            val filePath = obj.optString("filePath")
            if (filePath.isBlank()) continue
            result.add(
                buildItem(
                    id = obj.optString("id"),
                    title = obj.optString("title"),
                    artist = obj.optString("artist"),
                    album = obj.optString("album"),
                    filePath = filePath,
                    artworkUri = obj.optString("artworkUri").takeIf { it.isNotBlank() },
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
                    title = parts[1],
                    artist = parts[2],
                    album = parts[3],
                    filePath = parts[4],
                    artworkUri = parts.getOrNull(5)?.takeIf { it.isNotBlank() },
                )
            )
        }
        return result
    }

    private fun buildItem(
        id: String,
        title: String,
        artist: String,
        album: String,
        filePath: String,
        artworkUri: String?,
    ): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title.ifBlank { "Unknown" })
            .setArtist(artist.ifBlank { "Unknown" })
            .setAlbumTitle(album.ifBlank { "Unknown" })

        artworkUri?.let { metadataBuilder.setArtworkUri(Uri.parse(it)) }

        val bundle = Bundle().apply {
            putString("origin", "LOCAL")
            putString("file_path", filePath)
        }
        metadataBuilder.setExtras(bundle)

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(Uri.fromFile(java.io.File(filePath)).toString())
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }
}
