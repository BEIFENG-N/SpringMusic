package com.example.lxn2.data

object AppConstants {
    // 缓存与网络配置
    const val MEDIA_CACHE_SIZE_BYTES: Long = 512 * 1024 * 1024L // 512MB LRU 缓存上限
    const val HTTP_TIMEOUT_SECONDS: Long = 8L // QuickJS 网络请求超时秒数

    // ExoPlayer 缓冲配置 (针对 Wear OS 硬件调优)
    const val MIN_BUFFER_MS: Int = 10_000 // 最小缓冲 10 秒
    const val MAX_BUFFER_MS: Int = 30_000 // 最大缓冲 30 秒
    const val BUFFER_FOR_PLAYBACK_MS: Int = 2_000 // 起播缓冲 2 秒
    const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS: Int = 4_000 // 卡顿恢复缓冲 4 秒

    // 统一日志 Tag
    const val LOG_TAG: String = "LXN"
}
