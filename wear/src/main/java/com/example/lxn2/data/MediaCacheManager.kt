package com.example.lxn2.data

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Media3 磁盘缓存管理器。
 *
 * 为在线流媒体音频提供基于 LRU 的本地磁盘缓存，
 * 避免网络抖动、CDN 链接过期导致的中间暂停断流问题。
 */
@OptIn(UnstableApi::class)
object MediaCacheManager {

    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "media3_audio_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val evictor = LeastRecentlyUsedCacheEvictor(AppConstants.MEDIA_CACHE_SIZE_BYTES) // LRU 缓存上限
            val databaseProvider = StandaloneDatabaseProvider(context)
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }

    fun createCacheDataSourceFactory(
        context: Context,
        upstreamFactory: DataSource.Factory
    ): DataSource.Factory {
        val cache = getCache(context)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun getCacheSizeBytes(context: Context): Long {
        var totalBytes = 0L
        val dirs = listOf(
            File(context.cacheDir, "media3_audio_cache"),
            File(context.cacheDir, "covers"),
            File(context.cacheDir, "coil_cache")
        )
        for (dir in dirs) {
            if (dir.exists()) {
                totalBytes += getDirSize(dir)
            }
        }
        return totalBytes
    }

    private fun getDirSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) getDirSize(f) else f.length()
        }
        return size
    }

    @Synchronized
    fun clearCache(context: Context) {
        try {
            simpleCache?.let { cache ->
                val keys = cache.keys.toList()
                for (key in keys) {
                    cache.removeResource(key)
                }
            }
        } catch (e: Exception) {
            Log.e(AppConstants.LOG_TAG, "Error clearing Media3 SimpleCache", e)
        }

        val dirsToClear = listOf(
            File(context.cacheDir, "media3_audio_cache"),
            File(context.cacheDir, "covers"),
            File(context.cacheDir, "coil_cache")
        )
        for (dir in dirsToClear) {
            if (dir.exists()) {
                dir.deleteRecursively()
                dir.mkdirs()
            }
        }
    }
}
