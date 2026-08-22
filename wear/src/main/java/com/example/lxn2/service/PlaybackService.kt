package com.example.lxn2.service

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.lxn2.data.MediaRepository
import com.example.lxn2.data.PlaybackPersistence

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var persistence: PlaybackPersistence
    private lateinit var repository: MediaRepository

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d("LXN", "PlaybackService: onCreate")
        persistence = PlaybackPersistence(this)
        repository = MediaRepository(this)
        
        // Use a custom HttpDataSource to avoid 403 Forbidden on some online sources
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        
        // Restore playlist and position from cache
        val cachedMusic = repository.getCachedMusic()
        if (cachedMusic.isNotEmpty()) {
            player.setMediaItems(cachedMusic)
            
            val lastId = persistence.getLastPlayedIdFromZero()
            val lastIndex = persistence.getLastMediaIndex()
            
            // Try to match by ID first (safer across rescans), fallback to index
            val targetIndex = cachedMusic.indexOfFirst { it.mediaId == lastId }.takeIf { it != -1 } ?: lastIndex
            
            if (targetIndex in cachedMusic.indices) {
                player.seekTo(targetIndex, 0L)
            }
            player.prepare()
        }

        player.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (events.containsAny(
                            Player.EVENT_PLAY_WHEN_READY_CHANGED,
                            Player.EVENT_PLAYBACK_STATE_CHANGED,
                            Player.EVENT_MEDIA_ITEM_TRANSITION,
                            Player.EVENT_POSITION_DISCONTINUITY,
                        )
                    ) {
                        persistence.saveLastMediaIndex(player.currentMediaItemIndex)
                    }
                    
                    if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                        player.currentMediaItem?.mediaId?.let {
                            persistence.recordPlayedMedia(it)
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("LXN", "Player Error: ${error.errorCodeName} (${error.errorCode})", error)
                    Log.e("LXN", "Error Cause: ${error.cause?.message}")
                    error.cause?.printStackTrace()
                }
            },
        )

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d("LXN", "PlaybackService: onGetSession from ${controllerInfo.packageName}")
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            persistence.saveLastMediaIndex(player.currentMediaItemIndex)
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
