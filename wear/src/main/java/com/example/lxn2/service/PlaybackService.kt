package com.example.lxn2.service

import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.lxn2.data.AppConstants
import com.example.lxn2.data.MediaCacheCodec
import com.example.lxn2.data.MediaCacheManager
import com.example.lxn2.data.PlaybackPersistence
import com.example.lxn2.data.source.MusicSourceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var persistence: PlaybackPersistence
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d("LXN", "PlaybackService: onCreate")
        persistence = PlaybackPersistence(this)
        
        // 1. 网络 HTTP 数据源配置（防连接超时与断流）
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Linux; Android 12; SM-G9910) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://www.kuwo.cn/",
                    "Accept-Encoding" to "identity" 
                )
            )
        
        // 2. 本地 file:// 与网络全协议数据源
        val upstreamDataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        // 3. 集成 Media3 LRU 磁盘缓存 (512MB)，防止中途断流与 CDN 链接过期
        val cachedDataSourceFactory = MediaCacheManager.createCacheDataSourceFactory(this, upstreamDataSourceFactory)

        // 4. 缓冲策略调优 (小屏手表低功耗与内存压强优化)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                AppConstants.MIN_BUFFER_MS,
                AppConstants.MAX_BUFFER_MS,
                AppConstants.BUFFER_FOR_PLAYBACK_MS,
                AppConstants.BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cachedDataSourceFactory))
            .build()
        
        player.addListener(
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    persistence.saveLastMediaIndex(player.currentMediaItemIndex)
                    persistence.saveLastPosition(0L) // 新歌曲开始，位置重置
                }

                override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                    // 歌单变化时，在后台协程异步保存当前播放队列到 session
                    val items = mutableListOf<androidx.media3.common.MediaItem>()
                    for (i in 0 until player.mediaItemCount) {
                        items.add(player.getMediaItemAt(i))
                    }
                    serviceScope.launch {
                        val encoded = MediaCacheCodec.encode(items)
                        persistence.saveSessionPlaylist(encoded)
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    persistence.saveLastPosition(newPosition.positionMs)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e(AppConstants.LOG_TAG, "Player Error: ${error.errorCodeName} (${error.errorCode})", error)
                    
                    // 播放失败在主线程显式 Toast 提示用户
                    serviceScope.launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            applicationContext,
                            getString(com.example.lxn2.R.string.playback_error_msg),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }

                    // 核心修复：在线歌曲 CDN 链接过期或中途断流时，尝试重新请求新直链并续播
                    val currentItem = player.currentMediaItem
                    if (currentItem != null) {
                        val source = currentItem.mediaMetadata.extras?.getString("source")
                        val originalMediaId = currentItem.mediaId
                        if (!source.isNullOrEmpty() && originalMediaId.isNotBlank()) {
                            Log.d(AppConstants.LOG_TAG, "Attempting automatic URL refresh for online track: $originalMediaId")
                            serviceScope.launch {
                                try {
                                    val freshUrl = MusicSourceEngine.getMusicUrl(originalMediaId, source)
                                    if (freshUrl.isNotBlank()) {
                                        withContext(Dispatchers.Main) {
                                            val currentPos = player.currentPosition
                                            val newItem = currentItem.buildUpon().setUri(freshUrl.toUri()).build()
                                            player.setMediaItem(newItem, currentPos)
                                            player.prepare()
                                            player.play()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(AppConstants.LOG_TAG, "Failed to refresh URL on player error", e)
                                }
                            }
                        }
                    }
                }
            },
        )

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            persistence.saveLastMediaIndex(player.currentMediaItemIndex)
            persistence.saveLastPosition(player.currentPosition)
            persistence.flushPendingWrites()
            // 在 release 之前同步保存最新的播放队列，确保不因 serviceScope.cancel() 丢失
            val items = mutableListOf<androidx.media3.common.MediaItem>()
            for (i in 0 until player.mediaItemCount) {
                items.add(player.getMediaItemAt(i))
            }
            if (items.isNotEmpty()) {
                val encoded = MediaCacheCodec.encode(items)
                persistence.saveSessionPlaylist(encoded)
            }
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
