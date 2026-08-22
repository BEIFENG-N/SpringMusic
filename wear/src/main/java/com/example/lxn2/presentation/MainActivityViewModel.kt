package com.example.lxn2.presentation

import android.app.Application
import android.content.ComponentName
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.lxn2.data.MediaRepository
import com.example.lxn2.data.PlaybackPersistence
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

    private val _playerRepository = MutableStateFlow<PlayerRepository?>(null)
    val playerRepository: StateFlow<PlayerRepository?> = _playerRepository.asStateFlow()

    private val _isReady = MutableStateFlow(value = false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _remainingTime = MutableStateFlow<Long?>(null)
    val remainingTime: StateFlow<Long?> = _remainingTime.asStateFlow()

    private val _finishSongBeforeStop = MutableStateFlow(value = false)

    private var timerJob: kotlinx.coroutines.Job? = null

    private val persistence = PlaybackPersistence(application)
    private val repository = MediaRepository(application)

    init {
        connect()
    }

    fun connect() {
        if (controllerFuture?.isDone == false) return

        val app = getApplication<Application>()
        val sessionToken = SessionToken(app, ComponentName(app, com.example.lxn2.service.PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        
        controllerFuture?.addListener(
            {
                try {
                    val mediaControllerInstance = controllerFuture?.get()
                if (mediaControllerInstance != null) {
                    _mediaController.value = mediaControllerInstance
                    
                    if (mediaControllerInstance.mediaItemCount == 0) {
                        viewModelScope.launch {
                            val cachedItems = withContext(Dispatchers.IO) { repository.getCachedMusic() }
                            if (cachedItems.isNotEmpty()) {
                                Log.d("LXN", "ViewModel: Async loading ${cachedItems.size} items from cache.")
                                mediaControllerInstance.setMediaItems(cachedItems)
                                
                                val lastIdZero = persistence.getLastPlayedIdFromZero()
                                val index = cachedItems.indexOfFirst { it.mediaId == lastIdZero }
                                
                                if (index != -1) {
                                    mediaControllerInstance.seekTo(index, 0L)
                                } else {
                                    mediaControllerInstance.seekTo(0, 0L)
                                }
                                mediaControllerInstance.prepare()
                            }
                            _isReady.value = true
                        }
                    } else {
                        _isReady.value = true
                    }

                    mediaControllerInstance.addListener(
                        object : Player.Listener {
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                mediaItem?.mediaId?.let {
                                    persistence.recordPlayedMedia(it)
                                }
                            }
                        },
                    )

                    val repo = com.google.android.horologist.media.data.repository.PlayerRepositoryImpl()
                    repo.connect(mediaControllerInstance) {
                        _mediaController.value = null
                        _playerRepository.value = null
                        controllerFuture = null
                    }
                    _playerRepository.value = repo
                }
            } catch (e: Exception) {
                Log.e("LXN", "ViewModel: Connection failed", e)
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
            return
        }

        _remainingTime.value = totalSeconds.toLong()
        timerJob = viewModelScope.launch {
            var timeLeft = totalSeconds
            Log.d("LXN-Timer", "Starting timer: $timeLeft seconds")
            while (timeLeft > 0) {
                delay(1.seconds)
                timeLeft--
                _remainingTime.value = timeLeft.toLong()
            }
            Log.d("LXN-Timer", "Timer finished")
            
            if (_finishSongBeforeStop.value) {
                // 等待当前歌曲播放结束
                val controller = mediaController.value
                if (controller != null && controller.isPlaying) {
                    Log.d("LXN-Timer", "Time up! Waiting for current song to finish.")
                    _remainingTime.value = 0L // 用 0 表示等待中
                    
                    val listener = object : Player.Listener {
                        override fun onEvents(player: Player, events: Player.Events) {
                            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                                Log.d("LXN-Timer", "Song transition detected. Pausing now.")
                                player.pause()
                                player.removeListener(this)
                                _remainingTime.value = null
                            }
                        }
                    }
                    controller.addListener(listener)
                    return@launch
                }
            }
            
            mediaController.value?.pause()
            _remainingTime.value = null
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _remainingTime.value = null
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
