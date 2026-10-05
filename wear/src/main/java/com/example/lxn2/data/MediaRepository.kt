package com.example.lxn2.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.lxn2.data.source.MusicSourceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.json.JSONObject
import java.io.File

data class LyricLine(val timeMs: Long, val text: String, val translation: String? = null)

class MediaRepository(private val appContext: Context) {
    private val client = OkHttpClient()
    private val cacheFile = MediaCacheCodec.CACHE_FILE // v6 JSON 缓存（含封面信息）

    /**
     * Loads music from internal cache file. Extremely fast.
     */
    fun getCachedMusic(): List<MediaItem> {
        try {
            val file = File(appContext.filesDir, cacheFile)
            if (!file.exists()) {
                Log.d("LXN", "Repository: No cache file found.")
                return emptyList()
            }

            val content = file.readText()
            val musicList = MediaCacheCodec.decode(content)
            Log.d("LXN", "Repository: Loaded ${musicList.size} items from cache.")
            return musicList
        } catch (e: Exception) {
            Log.e("LXN", "Repository: Cache load error", e)
            return emptyList()
        }
    }

    /**
     * RECURSIVE scan with A-Z sorting and ID assignment from 1.
     */
    fun getMusicWithProgress(
        path: String,
        onProgress: (scanned: Int, total: Int) -> Unit,
    ): List<MediaItem> {
        val musicList = mutableListOf<MediaItem>()
        val rootDir = File(path)
        if (!rootDir.exists() || !rootDir.isDirectory) return emptyList()

        val allMusicFiles = mutableListOf<File>()
        findFilesRecursive(rootDir, allMusicFiles)
        
        // 1. A-Z Sorting by file name
        allMusicFiles.sortBy { it.name.lowercase() }
        
        val total = allMusicFiles.size
        if (total == 0) {
            onProgress(0, 0)
            return emptyList()
        }

        val coversDir = File(appContext.cacheDir, "covers")
        if (!coversDir.exists()) coversDir.mkdirs()

        // 2. Process and assign IDs 1, 2, 3...
        allMusicFiles.forEachIndexed { index, file ->
            var title = file.nameWithoutExtension
            var artist = "Unknown Artist"
            var album = "Unknown Album"
            var artworkPath: String? = null
            
            val numericId = (index + 1).toString()

            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let { title = it }
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let { artist = it }
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }?.let { album = it }
            } catch (e: Exception) { 
                Log.e(AppConstants.LOG_TAG, "Retriever metadata error for ${file.name}", e)
            } finally {
                try { retriever.release() } catch (ignored: Exception) {}
            }

            // 提取封面 (双重兜底：Retriever + JAudioTagger)
            val coverUri = getCoverUriForFilePath(appContext, file.absolutePath)
            if (coverUri != null) {
                artworkPath = coverUri.toString()
            }

            musicList.add(
                createMediaItem(
                    id = numericId, 
                    title = title,
                    artist = artist,
                    album = album,
                    uri = Uri.fromFile(file).toString(),
                    filePath = file.absolutePath,
                    artworkUri = artworkPath
                )
            )
            val currentProgress = index + 1
            if (currentProgress == 1 || currentProgress == total || currentProgress % 10 == 0) {
                onProgress(currentProgress, total)
            }
        }
        
        // 3. Save the definitive A-Z indexed cache (v6 JSON)
        saveCache(MediaCacheCodec.encode(musicList))
        
        return musicList
    }

    private fun findFilesRecursive(dir: File, result: MutableList<File>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) findFilesRecursive(file, result)
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext == "mp3" || ext == "wav" || ext == "flac") {
                result.add(file)
            }
            }
        }
    }

    private fun saveCache(data: String) {
        try {
            appContext.openFileOutput(cacheFile, Context.MODE_PRIVATE).use {
                it.write(data.toByteArray())
            }
            Log.d("LXN", "Repository: Cache saved successfully.")
        } catch (e: Exception) {
            Log.e("LXN", "Repository: Cache save error", e)
        }
    }

    private fun createMediaItem(
        id: String, 
        title: String?, 
        artist: String?, 
        album: String?, 
        uri: String, 
        filePath: String? = null,
        artworkUri: String? = null
    ): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title ?: "Unknown")
            .setArtist(artist ?: "Unknown")
            .setAlbumTitle(album ?: "Unknown")
        
        artworkUri?.let {
            metadataBuilder.setArtworkUri(Uri.parse(it))
        }
        
        val bundle = android.os.Bundle()
        bundle.putString("origin", "LOCAL")
        filePath?.let {
            bundle.putString("file_path", it)
        }
        metadataBuilder.setExtras(bundle)

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    suspend fun getParsedLyrics(mediaItem: MediaItem, showTranslation: Boolean = true): List<LyricLine> {
        val filePath = mediaItem.mediaMetadata.extras?.getString("file_path") 
            ?: mediaItem.localConfiguration?.uri?.path

        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                try {
                    val audioFile = AudioFileIO.read(file)
                    val tag = audioFile.tag ?: return emptyList()
                    val rawLyrics = tag.getFirst(FieldKey.LYRICS)
                    if (!rawLyrics.isNullOrBlank()) {
                        return LyricsParser.parse(lrcContent = rawLyrics, showTranslation = showTranslation)
                    }
                } catch (e: Exception) {
                    Log.e("LXN", "Local lyric error", e)
                }
            }
        }

        // Try JS Engine for online sources
        val source = mediaItem.mediaMetadata.extras?.getString("source")
        if (source != null) {
            return withContext(Dispatchers.IO) {
                try {
                    val lrcContent = MusicSourceEngine.getLyric(mediaItem.mediaId, source)
                    LyricsParser.parse(lrcContent = lrcContent, showTranslation = showTranslation)
                } catch (e: Exception) {
                    Log.e("LXN", "JS Engine lyric error", e)
                    emptyList()
                }
            }
        }

        // Try online lyrics if it's a Netease ID (numeric) - Fallback
        val mediaId = mediaItem.mediaId
        if (mediaId.all { it.isDigit() }) {
            return withContext(Dispatchers.IO) {
                try {
                    val url = "https://music.163.com/api/song/lyric?id=$mediaId&lv=1&kv=1&tv=-1"
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val lrc = json.optJSONObject("lrc")?.optString("lyric")
                    val tlrc = json.optJSONObject("tlyric")?.optString("lyric")
                    if (!lrc.isNullOrBlank()) {
                        LyricsParser.parse(lrcContent = lrc, tlyricContent = tlrc, showTranslation = showTranslation)
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    Log.e("LXN", "Online lyric error", e)
                    emptyList()
                }
            }
        }

        return emptyList()
    }


    fun getArtists(): List<String> {
        return getCachedMusic().asSequence().map { it.mediaMetadata.artist?.toString() ?: "Unknown Artist" }.distinct().sorted().toList()
    }

    fun getAlbums(): List<String> {
        return getCachedMusic().asSequence().map { it.mediaMetadata.albumTitle?.toString() ?: "Unknown Album" }.distinct().sorted().toList()
    }

    fun getMusicByArtist(artist: String): List<MediaItem> {
        return getCachedMusic().filter { (it.mediaMetadata.artist?.toString() ?: "Unknown Artist") == artist }
    }

    fun getMusicByAlbum(album: String): List<MediaItem> {
        return getCachedMusic().filter { (it.mediaMetadata.albumTitle?.toString() ?: "Unknown Album") == album }
    }

    companion object {
        fun extractCoverBytes(file: File): ByteArray? {
            if (!file.exists() || !file.isFile) return null
            // 1. 优先使用原生 MediaMetadataRetriever (C++ 底层解解析)
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val picture = retriever.embeddedPicture
                if (picture != null && picture.isNotEmpty()) {
                    return picture
                }
            } catch (e: Exception) {
                // 忽略原生 Retriever 异常，后面走 JAudioTagger 兜底
            } finally {
                try { retriever.release() } catch (ignored: Exception) {}
            }

            // 2. 兜底方案：使用 JAudioTagger 解析非标 ID3v2/FLAC 标签中的 Artwork
            return try {
                val audioFile = AudioFileIO.read(file)
                audioFile.tag?.firstArtwork?.binaryData
            } catch (e: Exception) {
                null
            }
        }

        fun getCoverUriForFilePath(context: Context, filePath: String): Uri? {
            if (filePath.isBlank()) return null
            val file = File(filePath)
            if (!file.exists() || !file.isFile) return null

            val coversDir = File(context.cacheDir, "covers")
            if (!coversDir.exists()) coversDir.mkdirs()

            val coverFileName = "${file.name.hashCode()}_cover.jpg"
            val coverFile = File(coversDir, coverFileName)
            if (coverFile.exists() && coverFile.length() > 0) {
                return Uri.fromFile(coverFile)
            }

            val picture = extractCoverBytes(file) ?: return null
            return try {
                coverFile.writeBytes(picture)
                Uri.fromFile(coverFile)
            } catch (e: Exception) {
                null
            }
        }

        fun getCoverUriForMediaItem(context: Context, mediaItem: MediaItem?): Uri? {
            if (mediaItem == null) return null
            val meta = mediaItem.mediaMetadata

            // 1. 检查既有 artworkUri 在磁盘上是否有效存在
            meta.artworkUri?.let { uri ->
                val scheme = uri.scheme
                if (scheme == "http" || scheme == "https") {
                    return uri
                }
                val path = uri.path
                if (!path.isNullOrBlank()) {
                    val coverFile = File(path)
                    if (coverFile.exists() && coverFile.length() > 0) {
                        return uri
                    }
                }
            }

            // 2. 若既有 artworkUri 为空或磁盘文件被清理，根据 filePath 重新实时解包封面
            val filePath = meta.extras?.getString("file_path")
                ?: mediaItem.localConfiguration?.uri?.path
            if (!filePath.isNullOrBlank()) {
                return getCoverUriForFilePath(context, filePath)
            }

            return null
        }
    }
}
