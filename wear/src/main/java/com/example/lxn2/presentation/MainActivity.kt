@file:OptIn(ExperimentalHorologistApi::class)
package com.example.lxn2.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.lxn2.LXNApplication
import com.example.lxn2.presentation.navigation.WearNavHost
import com.example.lxn2.presentation.theme.LXNTheme
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumeViewModel
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
            var currentThemeColor by remember { mutableStateOf(Color(persistence.getThemeColor())) }

            LXNTheme(primaryColor = currentThemeColor) {
                AppScaffold {
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
                        onStartTimer = { totalSec, finishSong -> activityViewModel.startShutdownTimer(totalSec, finishSong) },
                        onCancelTimer = { activityViewModel.cancelTimer() },
                        remainingTimeFlow = remainingTimeFlow,
                    ) { color ->
                        currentThemeColor = color
                        persistence.saveThemeColor(color.toArgb().toLong())
                    }
                }
            }
        }
    }
}
