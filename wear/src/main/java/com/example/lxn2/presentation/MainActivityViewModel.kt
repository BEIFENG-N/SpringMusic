package com.example.lxn2.presentation

import android.app.Application
import android.content.ComponentName
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.lxn2.LXNApplication
import com.example.lxn2.data.AppConstants
import com.example.lxn2.data.MediaCacheCodec
import com.example.lxn2.service.PlaybackService
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.repository.PlayerRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalHorologistApi::class)
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    
    private val _mediaController = MutableStateFlow<MediaController?>(null)
    val mediaController: StateFlow<MediaController?> = _mediaController.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _playerRepository = MutableStateFlow<PlayerRepository?>(null)
    val playerRepository: StateFlow<PlayerRepository?> = _playerRepository.asStateFlow()

    private val _isReady = MutableStateFlow(value = false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _remainingTime = MutableStateFlow<Long?>(null)
    val remainingTime: StateFlow<Long?> = _remainingTime.asStateFlow()

    private val _finishSongBeforeStop = MutableStateFlow(value = false)

    private var timerJob: kotlinx.coroutines.Job? = null

    private val container = (application as LXNApplication).container
    private val persistence = container.playbackPersistence
    private val repository = container.mediaRepository

    init {
        connect()
    }

    fun connect() {
        if (controllerFuture?.isDone == false) return

        val app = getApplication<Application>()
        val sessionToken = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        
        controllerFuture?.addListener(
            {
                try {
                    val mediaControllerInstance = controllerFuture?.get()
                if (mediaControllerInstance != null) {
                    _mediaController.value = mediaControllerInstance
                    _currentMediaItem.value = mediaControllerInstance.currentMediaItem
                    
                    if (mediaControllerInstance.mediaItemCount == 0) {
                        viewModelScope.launch {
                            // 优先从 session 恢复（包含在线和本地混合队列）
                            val sessionJson = persistence.getSessionPlaylist()
                            val restoredItems = if (sessionJson != null) {
                                MediaCacheCodec.decode(sessionJson)
                            } else {
                                withContext(Dispatchers.IO) { repository.getCachedMusic() }
                            }

                            if (restoredItems.isNotEmpty()) {
                                Log.d(AppConstants.LOG_TAG, "ViewModel: Restoring ${restoredItems.size} items from session/cache.")
                                mediaControllerInstance.setMediaItems(restoredItems)
                                
                                val lastIndex = persistence.getLastMediaIndex()
                                val lastPos = persistence.getLastPosition()
                                
                                if (lastIndex in restoredItems.indices) {
                                    mediaControllerInstance.seekTo(lastIndex, lastPos)
                                } else {
                                    mediaControllerInstance.seekTo(0, 0L)
                                }
                                mediaControllerInstance.prepare()
                            }
                            _currentMediaItem.value = mediaControllerInstance.currentMediaItem
                            _isReady.value = true
                        }
                    } else {
                        _isReady.value = true
                    }

                    mediaControllerInstance.addListener(
                        object : Player.Listener {
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                _currentMediaItem.value = mediaItem ?: mediaControllerInstance.currentMediaItem
                                mediaItem?.let {
                                    persistence.recordPlayedMedia(it.mediaId)
                                    com.example.lxn2.data.core.PlayerCore.addPlayedList(it)
                                    
                                    // Handle timeout exit (isPlayedStop)
                                    if (com.example.lxn2.data.core.PlayerCore.isPlayedStop.value) {
                                        Log.d(AppConstants.LOG_TAG, "isPlayedStop is true, stopping playback after song finish.")
                                        mediaControllerInstance.pause()
                                        com.example.lxn2.data.core.PlayerCore.setIsPlayedStop(false)
                                        _remainingTime.value = null
                                    }
                                }
                            }
                        },
                    )

                    val repo = PlayerRepositoryImpl()
                    repo.connect(mediaControllerInstance) {
                        _mediaController.value = null
                        _playerRepository.value = null
                        controllerFuture = null
                    }
                    _playerRepository.value = repo
                }
            } catch (e: Exception) {
                Log.e(AppConstants.LOG_TAG, "ViewModel: Connection failed", e)
                controllerFuture = null 
                _isReady.value = true
            }
        }, MoreExecutors.directExecutor())
    }

    fun startShutdownTimer(totalSeconds: Int, finishSong: Boolean) {
        timerJob?.cancel()
        _finishSongBeforeStop.value = finishSong
        if (totalSeconds <= 0) {
            _remainingTime.value = null
            com.example.lxn2.data.core.PlayerCore.setIsPlayedStop(false)
            return
        }

        _remainingTime.value = totalSeconds.toLong()
        timerJob = viewModelScope.launch {
            var timeLeft = totalSeconds
            Log.d(AppConstants.LOG_TAG, "Starting timer: $timeLeft seconds")
            while (timeLeft > 0) {
                delay(1.seconds)
                timeLeft--
                _remainingTime.value = timeLeft.toLong()
            }
            Log.d(AppConstants.LOG_TAG, "Timer finished")
            
            if (_finishSongBeforeStop.value) {
                // 等待当前歌曲播放结束
                val controller = mediaController.value
                if (controller != null && controller.isPlaying) {
                    Log.d(AppConstants.LOG_TAG, "Time up! Waiting for current song to finish.")
                    _remainingTime.value = 0L // 用 0 表示等待中
                    com.example.lxn2.data.core.PlayerCore.setIsPlayedStop(true)
                    return@launch
                }
            }
            
            mediaController.value?.pause()
            _remainingTime.value = null
            com.example.lxn2.data.core.PlayerCore.setIsPlayedStop(false)
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _remainingTime.value = null
        com.example.lxn2.data.core.PlayerCore.setIsPlayedStop(false)
    }

    // --- Temp Play List Integration ---
    fun removeFromTempPlayList(index: Int) {
        com.example.lxn2.data.core.PlayerCore.removeTempPlayList(index)
    }

    fun clearTempPlayList() {
        com.example.lxn2.data.core.PlayerCore.clearTempPlayList()
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
