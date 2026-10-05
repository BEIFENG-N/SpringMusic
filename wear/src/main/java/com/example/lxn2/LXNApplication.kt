package com.example.lxn2

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.lxn2.di.AppContainer

/**
 * 应用级入口，持有应用级依赖容器 [AppContainer]。
 * 实现 [ImageLoaderFactory] 为 Wear OS 配置内存与磁盘缓存限制，防止高清封面导致的内存溢出 (OOM)。
 */
class LXNApplication : Application(), ImageLoaderFactory {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // 使用可用内存的 15% 作为 Coil 缓存上限
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(10 * 1024 * 1024L) // 10MB 磁盘缓存
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}
