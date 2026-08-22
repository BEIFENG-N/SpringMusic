package com.example.lxn2.presentation

import android.app.Application
import android.content.ComponentName
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
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

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _remainingTime = MutableStateFlow<Long?>(null)
    val remainingTime: StateFlow<Long?> = _remainingTime.asStateFlow()

    private val _finishSongBeforeStop = MutableStateFlow(false)
    val finishSongBeforeStop: StateFlow<Boolean> = _finishSongBeforeStop.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        connect()
    }

    fun connect() {
        if (controllerFuture?.isDone == false) return

        val app = getApplication<Application>()
        val sessionToken = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                if (controller != null) {
                    _mediaController.value = controller
                    
                    // 同步初始定时器状态（如果需要）
                    _isReady.value = true

                    val repo = PlayerRepositoryImpl()
                    repo.connect(controller) {
                        _mediaController.value = null
                        _playerRepository.value = null
                        controllerFuture = null
                    }
                    _playerRepository.value = repo
                }
            } catch (e: Exception) {
                Log.e("LXN", "ViewModel: Connection failed", e)
                controllerFuture = null 
                _isReady.value = true // 即使失败也让启动页消失
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
        super.onCleared()
    }
}
