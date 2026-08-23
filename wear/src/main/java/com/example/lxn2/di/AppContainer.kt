package com.example.lxn2.di

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lxn2.data.MediaRepository
import com.example.lxn2.data.PlaybackPersistence
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.media.repository.PlayerRepository
import com.google.android.horologist.media.ui.state.PlayerViewModel

/**
 * 轻量应用级依赖容器。
 *
 * 统一提供 [playbackPersistence] 与 [mediaRepository] 单例，
 * 并负责创建 Horologist 的 [VolumeViewModel] / [PlayerViewModel]，
 * 替代原先散落在各处的 ViewModel Factory。
 */
@OptIn(ExperimentalHorologistApi::class)
class AppContainer(private val context: Context) {

    val playbackPersistence: PlaybackPersistence by lazy { PlaybackPersistence(context) }

    val mediaRepository: MediaRepository by lazy { MediaRepository(context) }

    fun createVolumeViewModelFactory(): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = SystemAudioRepository.fromContext(context)
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                        .defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                return VolumeViewModel(repository, repository, onCleared = {}, vibrator = vibrator) as T
            }
        }

    fun createPlayerViewModel(playerRepository: PlayerRepository): PlayerViewModel =
        PlayerViewModel(playerRepository)
}
