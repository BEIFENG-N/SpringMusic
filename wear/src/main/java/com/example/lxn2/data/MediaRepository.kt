package com.example.lxn2.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.json.JSONObject
import java.io.File

data class LyricLine(val timeMs: Long, val text: String)

class MediaRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val cacheFile = "music_cache_v5.txt" // 升级到 v5 以包含封面信息

    /**
     * Loads music from internal cache file. Extremely fast.
     */
    fun getCachedMusic(): List<MediaItem> {
        val musicList = mutableListOf<MediaItem>()
        try {
            val file = File(context.filesDir, cacheFile)
            if (!file.exists()) {
                Log.d("LXN", "Repository: No cache file found.")
                return emptyList()
            }

            file.readLines().forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 5) {
                    val id = parts[0]
                    val title = parts[1]
                    val artist = parts[2]
                    val album = parts[3]
                    val path = parts[4]
                    val artworkUri = if (parts.size >= 6) parts[5] else null
                    
                    musicList.add(
                        createMediaItem(
                            id = id, 
                            title = title, 
                            artist = artist, 
                            album = album, 
                            uri = Uri.fromFile(File(path)).toString(), 
                            filePath = path,
                            artworkUri = artworkUri
                        )
                    )
                }
            }
            Log.d("LXN", "Repository: Loaded ${musicList.size} items from cache.")
        } catch (e: Exception) {
            Log.e("LXN", "Repository: Cache load error", e)
        }
        return musicList
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

        val cacheData = StringBuilder()
        val coversDir = File(context.cacheDir, "covers")
        if (!coversDir.exists()) coversDir.mkdirs()

        // 2. Process and assign IDs 1, 2, 3...
        allMusicFiles.forEachIndexed { index, file ->
            var title = file.nameWithoutExtension
            var artist = "Unknown Artist"
            var album = "Unknown Album"
            var artworkPath: String? = null
            
            val numericId = (index + 1).toString()

            try {
                val audioFile = AudioFileIO.read(file)
                val tag = audioFile.tag
                if (tag != null) {
                    tag.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() }?.let { title = it }
                    tag.getFirst(FieldKey.ARTIST)?.takeIf { it.isNotBlank() }?.let { artist = it }
                    tag.getFirst(FieldKey.ALBUM)?.takeIf { it.isNotBlank() }?.let { album = it }
                    
                    // 识别并提取封面
                    val artwork = tag.firstArtwork
                    if (artwork != null) {
                        val coverFile = File(coversDir, "$numericId.jpg")
                        // 简单起见，每次扫描都重新提取，或者检查文件是否存在
                        if (!coverFile.exists()) {
                            coverFile.writeBytes(artwork.binaryData)
                        }
                        artworkPath = Uri.fromFile(coverFile).toString()
                    }
                }
            } catch (e: Exception) { 
                Log.e("LXN", "Tag read error for ${file.name}", e)
            }

            cacheData.append("$numericId|$title|$artist|$album|${file.absolutePath}|${artworkPath ?: ""}\n")

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
            onProgress(index + 1, total)
        }
        
        // 3. Save the definitive A-Z indexed cache
        saveCache(cacheData.toString())
        
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
            context.openFileOutput(cacheFile, Context.MODE_PRIVATE).use {
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

    suspend fun getParsedLyrics(mediaItem: MediaItem): List<LyricLine> {
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
                        return parseLrc(rawLyrics)
                    }
                } catch (e: Exception) {
                    Log.e("LXN", "Local lyric error", e)
                }
            }
        }

        // Try online lyrics if it's a Netease ID (numeric)
        val mediaId = mediaItem.mediaId
        if (mediaId.all { it.isDigit() }) {
            return withContext(Dispatchers.IO) {
                try {
                    val url = "https://music.163.com/api/song/lyric?id=$mediaId&lv=1&kv=1&tv=-1"
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val lrc = json.optJSONObject("lrc")?.optString("lyric")
                    if (!lrc.isNullOrBlank()) {
                        parseLrc(lrc)
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

    private fun parseLrc(lrcContent: String): List<LyricLine> {
        val lyrics = mutableListOf<LyricLine>()
        val timeRegex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})]".toRegex()
        lrcContent.lines().forEach { line ->
            val matchResult = timeRegex.find(line)
            if (matchResult != null) {
                val min = matchResult.groupValues[1].toLong()
                val sec = matchResult.groupValues[2].toLong()
                val msStr = matchResult.groupValues[3]
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                val totalTimeMs = (min * 60 + sec) * 1000 + ms
                val text = line.replace(timeRegex, "").trim()
                if (text.isNotEmpty()) lyrics.add(LyricLine(totalTimeMs, text))
            }
        }
        return lyrics.sortedBy { it.timeMs }
    }

    fun getAllMusic(customPath: String? = null): List<MediaItem> {
        return getCachedMusic()
    }

    fun getArtists(): List<String> {
        return getCachedMusic().map { it.mediaMetadata.artist?.toString() ?: "Unknown Artist" }.distinct().sorted()
    }

    fun getAlbums(): List<String> {
        return getCachedMusic().map { it.mediaMetadata.albumTitle?.toString() ?: "Unknown Album" }.distinct().sorted()
    }

    fun getMusicByArtist(artist: String): List<MediaItem> {
        return getCachedMusic().filter { (it.mediaMetadata.artist?.toString() ?: "Unknown Artist") == artist }
    }

    fun getMusicByAlbum(album: String): List<MediaItem> {
        return getCachedMusic().filter { (it.mediaMetadata.albumTitle?.toString() ?: "Unknown Album") == album }
    }
}
