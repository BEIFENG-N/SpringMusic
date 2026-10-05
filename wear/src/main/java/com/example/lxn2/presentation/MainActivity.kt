@file:OptIn(ExperimentalHorologistApi::class)
package com.example.lxn2.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import coil.compose.AsyncImage
import com.example.lxn2.LXNApplication
import com.example.lxn2.presentation.navigation.WearNavHost
import com.example.lxn2.presentation.permission.PermissionScreen
import com.example.lxn2.presentation.theme.LXNTheme
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.images.coil.CoilPaintable
import com.google.android.horologist.media.ui.components.background.ArtworkColorBackground
import com.google.android.horologist.media.ui.state.PlayerViewModel

@OptIn(ExperimentalHorologistApi::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val activityViewModel: MainActivityViewModel = viewModel<MainActivityViewModel>()
            val mediaController by activityViewModel.mediaController.collectAsStateWithLifecycle()
            val playerRepository by activityViewModel.playerRepository.collectAsStateWithLifecycle()
            val isReady by activityViewModel.isReady.collectAsStateWithLifecycle()

            splashScreen.setKeepOnScreenCondition { !isReady }

            val appContainer = (application as LXNApplication).container
            val persistence = remember { appContainer.playbackPersistence }

            // 初始化 JS 音源引擎
            androidx.compose.runtime.LaunchedEffect(Unit) {
                val activeScriptPath = persistence.getActiveSourcePath()
                com.example.lxn2.data.source.MusicSourceEngine.initEngine(applicationContext, activeScriptPath)
            }

            var currentThemeColor by remember { mutableStateOf(Color(persistence.getThemeColor())) }
            var backgroundBlur by rememberSaveable { mutableStateOf(persistence.isBackgroundBlurEnabled()) }
            var displayEffect by rememberSaveable { mutableStateOf(persistence.getDisplayEffect()) }
            var appScale by rememberSaveable { mutableStateOf(persistence.getAppScale()) }

            val currentDensity = LocalDensity.current
            val scaledDensity = remember(currentDensity, appScale) {
                Density(
                    density = currentDensity.density * appScale,
                    fontScale = currentDensity.fontScale * appScale
                )
            }

            // 监听当前播放项目（由 ViewModel 统一维护单一数据源）
            val currentMediaItem by activityViewModel.currentMediaItem.collectAsStateWithLifecycle()

            // 首次启动先展示权限说明...
            var permissionResolved by rememberSaveable {
                mutableStateOf(persistence.isPermissionPromptShown())
            }
            val markPermissionResolved: () -> Unit = {
                persistence.markPermissionPromptShown()
                permissionResolved = true
            }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                LXNTheme(primaryColor = currentThemeColor, displayEffect = displayEffect) {
                    AppScaffold(
                        timeText = { }
                    ) {
                        if (!permissionResolved) {
                            PermissionScreen(
                                onGranted = markPermissionResolved,
                                onSkipped = markPermissionResolved
                            )
                            return@AppScaffold
                        }

                        val navController = rememberSwipeDismissableNavController()
                        val playerViewModel: PlayerViewModel? = remember(playerRepository) {
                            playerRepository?.let { appContainer.createPlayerViewModel(it) }
                        }
                        val volumeViewModel: VolumeViewModel = viewModel<VolumeViewModel>(
                            factory = appContainer.createVolumeViewModelFactory(),
                        )

                        val remainingTimeFlow = activityViewModel.remainingTime

                        WearNavHost(
                            navController = navController, 
                            controller = mediaController,
                            playerViewModel = playerViewModel,
                            volumeViewModel = volumeViewModel,
                            mainActivityViewModel = activityViewModel,
                            onStartTimer = { totalSec, finishSong -> activityViewModel.startShutdownTimer(totalSec, finishSong) },
                            onCancelTimer = { activityViewModel.cancelTimer() },
                            remainingTimeFlow = remainingTimeFlow,
                            backgroundBlur = backgroundBlur,
                            onBackgroundBlurChange = { enabled ->
                                backgroundBlur = enabled
                                persistence.saveBackgroundBlur(enabled)
                            },
                            displayEffect = displayEffect,
                            onDisplayEffectChange = { effect ->
                                displayEffect = effect
                                persistence.saveDisplayEffect(effect)
                            },
                            appScale = appScale,
                            onAppScaleChange = { scale ->
                                appScale = scale
                                persistence.saveAppScale(scale)
                            }
                        ) { color ->
                            currentThemeColor = color
                            persistence.saveThemeColor(color.toArgb().toLong())
                        }
                    }
                }
            }
        }
    }
}
