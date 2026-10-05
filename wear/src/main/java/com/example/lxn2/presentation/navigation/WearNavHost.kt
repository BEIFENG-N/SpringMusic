@file:OptIn(ExperimentalHorologistApi::class)
package com.example.lxn2.presentation.navigation

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.media3.common.MediaItem
import coil.request.ImageRequest
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import coil.compose.AsyncImage
import com.example.lxn2.data.MediaRepository
import com.example.lxn2.data.PlaybackPersistence
import com.example.lxn2.permission.PermissionManager
import com.example.lxn2.presentation.MainActivityViewModel
import com.example.lxn2.presentation.library.AlbumsScreen
import com.example.lxn2.presentation.library.ArtistsScreen
import com.example.lxn2.presentation.library.FilteredMusicScreen
import com.example.lxn2.presentation.library.LibraryScreen
import com.example.lxn2.presentation.library.PlaylistScreen
import com.example.lxn2.presentation.library.SearchScreen
import com.example.lxn2.presentation.player.LyricsScreenContent
import com.example.lxn2.presentation.player.OutputDevicePickerScreen
import com.example.lxn2.presentation.player.PlayerUI
import com.example.lxn2.presentation.player.TempPlayListScreen
import com.example.lxn2.presentation.settings.AppInterfaceSettingsScreen
import com.example.lxn2.presentation.settings.CacheManagementScreen
import com.example.lxn2.presentation.settings.DisplayEffectSettingsScreen
import com.example.lxn2.presentation.settings.DisplayEffectSettingsScreen
import com.example.lxn2.presentation.settings.LyricsSettingsScreen
import com.example.lxn2.presentation.settings.MoreScreen
import com.example.lxn2.presentation.settings.PlayerSettingsScreen
import com.example.lxn2.presentation.settings.ScaleSettingsScreen
import com.example.lxn2.presentation.settings.SettingsScreen
import com.example.lxn2.presentation.settings.ThemeColorPickerScreen
import com.example.lxn2.presentation.settings.ThemeSettingsScreen
import com.example.lxn2.presentation.source.FolderPickerScreen
import com.example.lxn2.presentation.source.JsFilePickerScreen
import com.example.lxn2.presentation.source.LocalMusicScanMenuScreen
import com.example.lxn2.presentation.source.SourceManagementScreen
import com.example.lxn2.presentation.timer.ScanningOverlay
import com.example.lxn2.presentation.timer.TimePickerScreen
import com.example.lxn2.presentation.timer.TimerSetupScreen
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.images.coil.CoilPaintable
import com.google.android.horologist.media.ui.components.background.ArtworkColorBackground
import com.google.android.horologist.media.ui.state.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LocalCurrentMediaItem = staticCompositionLocalOf<MediaItem?> { null }

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun WearNavHost(
    navController: NavHostController, 
    controller: MediaController?,
    playerViewModel: PlayerViewModel?,
    volumeViewModel: VolumeViewModel,
    mainActivityViewModel: MainActivityViewModel? = null,
    onStartTimer: (Int, Boolean) -> Unit,
    onCancelTimer: () -> Unit,
    remainingTimeFlow: StateFlow<Long?>,
    backgroundBlur: Boolean,
    onBackgroundBlurChange: (Boolean) -> Unit,
    displayEffect: String = "GLASSMORPHISM",
    onDisplayEffectChange: (String) -> Unit = {},
    appScale: Float = 1.0f,
    onAppScaleChange: (Float) -> Unit = {},
    onThemeColorChange: (Color) -> Unit
) {
    val appContext = LocalContext.current
    val persistence = remember { PlaybackPersistence(appContext) }
    
    var selectedFolderPath by rememberSaveable { mutableStateOf(persistence.getLastPath()) }
    var scanTriggerCount by rememberSaveable { mutableIntStateOf(0) }
    var lastAppliedPath by remember { mutableStateOf<String?>(null) }
    var lastHandledScanTrigger by rememberSaveable { mutableIntStateOf(0) }
    var detectEmbeddedLyrics by rememberSaveable { mutableStateOf(true) }

    val verticalPagerState = rememberPagerState(initialPage = 0) { 2 }
    val horizontalPagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    val currentMediaItem by (mainActivityViewModel?.currentMediaItem ?: remember { kotlinx.coroutines.flow.MutableStateFlow(null) }).collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalCurrentMediaItem provides currentMediaItem) {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "player"
        ) {
            // 定义一个内部组件，为每个导航页面提供背景。
            // 由于每个页面都有了不透明的背景，上一级界面的内容将被完全遮挡，解决重叠问题。
            @Composable
            fun PageWrapper(content: @Composable () -> Unit) {
                val context = LocalContext.current
                val mediaItem = LocalCurrentMediaItem.current
                val artworkUri = remember(mediaItem?.mediaId, mediaItem?.mediaMetadata?.artworkUri) {
                    MediaRepository.getCoverUriForMediaItem(context, mediaItem)
                }

                val imageRequest = remember(artworkUri) {
                    artworkUri?.let { uri ->
                        ImageRequest.Builder(context)
                            .data(uri)
                            .size(400, 400)
                            .crossfade(true)
                            .build()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (artworkUri != null) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (backgroundBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Modifier.blur(28.dp)
                                    } else Modifier
                                ),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Color.Black.copy(
                                        alpha = if (backgroundBlur) 0.45f else 0.35f
                                    )
                                )
                        )
                    } else {
                        // 无封面或未开始播放时：显示主题暗色容器背景，而不是死沉的全黑
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                        )
                    }

                    // 2. 核心 UI 内容层
                    content()
                }
            }

        composable("player") {
            PageWrapper {
                MainPagerScreen(
                    navController = navController,
                    controller = controller, 
                    playerViewModel = playerViewModel,
                    volumeViewModel = volumeViewModel,
                    mainActivityViewModel = mainActivityViewModel,
                    onOutputClick = { navController.navigate("output_device_picker") },
                    onLibraryClick = { navController.navigate("library") },
                    onTimerClick = { navController.navigate("timer_setup") },
                    remainingTimeFlow = remainingTimeFlow,
                    customPath = selectedFolderPath,
                    scanTrigger = scanTriggerCount,
                    lastHandledScanTrigger = lastHandledScanTrigger,
                    onScanHandled = { lastHandledScanTrigger = it },
                    onPathApplied = { 
                        lastAppliedPath = it
                        persistence.saveLastPath(it)
                    },
                    persistence = persistence,
                    detectLyrics = detectEmbeddedLyrics,
                    backgroundBlur = backgroundBlur,
                    verticalPagerState = verticalPagerState,
                    horizontalPagerState = horizontalPagerState
                )
            }
        }
        composable("timer_setup") {
            PageWrapper {
                var setupHours by rememberSaveable { mutableIntStateOf(0) }
                var setupMinutes by rememberSaveable { mutableIntStateOf(5) }
                var setupSeconds by rememberSaveable { mutableIntStateOf(0) }
                
                TimerSetupScreen(
                    initialHours = setupHours,
                    initialMinutes = setupMinutes,
                    initialSeconds = setupSeconds,
                    onTimeClick = { navController.navigate("time_picker") },
                    onConfirm = { seconds, finishSong ->
                        onStartTimer(seconds, finishSong)
                        navController.popBackStack()
                    },
                    onCancel = {
                        onCancelTimer()
                        navController.popBackStack()
                    },
                    navController = navController
                )
            }
        }
        composable("time_picker") {
            PageWrapper {
                TimePickerScreen(
                    onConfirm = { h, m, s ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("h", h)
                        navController.previousBackStackEntry?.savedStateHandle?.set("m", m)
                        navController.previousBackStackEntry?.savedStateHandle?.set("s", s)
                        navController.popBackStack()
                    }
                )
            }
        }
        composable("playlist") {
            PageWrapper {
                PlaylistScreen(
                    controller = controller,
                    persistence = persistence,
                    onDismiss = {
                        scope.launch { verticalPagerState.scrollToPage(0) }
                        navController.popBackStack("player", inclusive = false)
                    }
                )
            }
        }
        composable("search") {
            PageWrapper {
                SearchScreen(
                    controller = controller,
                    persistence = persistence,
                    onDismiss = { 
                        scope.launch { verticalPagerState.scrollToPage(0) }
                        navController.popBackStack("player", inclusive = false) 
                    }
                )
            }
        }
        composable("library") {
            PageWrapper {
                LibraryScreen(
                    onLocalMusicClick = { navController.navigate("playlist") },
                    onTempPlayListClick = { navController.navigate("temp_playlist") },
                    onArtistsClick = { navController.navigate("artists") },
                    onAlbumsClick = { navController.navigate("albums") }
                )
            }
        }
        composable("temp_playlist") {
            PageWrapper {
                TempPlayListScreen(
                    onRemove = { mainActivityViewModel?.removeFromTempPlayList(it) },
                    onClear = { mainActivityViewModel?.clearTempPlayList() }
                )
            }
        }
        composable("artists") {
            PageWrapper {
                ArtistsScreen(
                    onArtistClick = { artist -> navController.navigate("artist_songs/$artist") }
                )
            }
        }
        composable(
            route = "artist_songs/{artistName}",
            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
        ) { backStackEntry ->
            PageWrapper {
                val artistName = backStackEntry.arguments?.getString("artistName") ?: ""
                FilteredMusicScreen(
                    title = artistName,
                    filterType = "artist",
                    filterValue = artistName,
                    controller = controller,
                    persistence = persistence,
                    onDismiss = { 
                        scope.launch { verticalPagerState.scrollToPage(0) }
                        navController.popBackStack("player", inclusive = false) 
                    }
                )
            }
        }
        composable("albums") {
            PageWrapper {
                AlbumsScreen(
                    onAlbumClick = { album -> navController.navigate("album_songs/$album") }
                )
            }
        }
        composable(
            route = "album_songs/{albumName}",
            arguments = listOf(navArgument("albumName") { type = NavType.StringType })
        ) { backStackEntry ->
            PageWrapper {
                val albumName = backStackEntry.arguments?.getString("albumName") ?: ""
                FilteredMusicScreen(
                    title = albumName,
                    filterType = "album",
                    filterValue = albumName,
                    controller = controller,
                    persistence = persistence,
                    onDismiss = { 
                        scope.launch { verticalPagerState.scrollToPage(0) }
                        navController.popBackStack("player", inclusive = false) 
                    }
                )
            }
        }
        composable("settings") {
            PageWrapper {
                SettingsScreen(
                    onScanLocalMusicClick = { navController.navigate("local_music_scan_menu") },
                    onPlayerSettingsClick = { navController.navigate("player_settings") },
                    onSourceManagementClick = { navController.navigate("source_management") },
                    onLyricsSettingsClick = { navController.navigate("lyrics_settings") },
                    onAppInterfaceClick = { navController.navigate("app_interface_settings") },
                    onCacheManagementClick = { navController.navigate("cache_management") }
                )
            }
        }
        composable("cache_management") {
            PageWrapper {
                CacheManagementScreen()
            }
        }
        composable("app_interface_settings") {
            PageWrapper {
                AppInterfaceSettingsScreen(
                    onThemeSettingsClick = { navController.navigate("theme_settings") },
                    onScaleSettingsClick = { navController.navigate("scale_settings") }
                )
            }
        }
        composable("scale_settings") {
            PageWrapper {
                ScaleSettingsScreen(
                    currentScale = appScale,
                    onScaleChange = onAppScaleChange
                )
            }
        }
        composable("player_settings") {
            PageWrapper {
                PlayerSettingsScreen(
                    persistence = persistence,
                    controller = controller
                )
            }
        }
        composable("theme_settings") {
            PageWrapper {
                ThemeSettingsScreen(
                    onDisplayEffectClick = { navController.navigate("display_effect_settings") },
                    onThemeColorClick = { navController.navigate("theme_color_picker") }
                )
            }
        }
        composable("display_effect_settings") {
            PageWrapper {
                DisplayEffectSettingsScreen(
                    blurEnabled = backgroundBlur,
                    onToggleBlur = onBackgroundBlurChange,
                    displayEffect = displayEffect,
                    onDisplayEffectChange = onDisplayEffectChange
                )
            }
        }
        composable("theme_color_picker") {
            PageWrapper {
                ThemeColorPickerScreen(
                    onColorSelected = { color ->
                        onThemeColorChange(color)
                        navController.popBackStack()
                    }
                )
            }
        }
        composable("source_management") {
            PageWrapper {
                SourceManagementScreen(
                    persistence = persistence,
                    onAddSourceClick = { navController.navigate("js_file_picker") }
                )
            }
        }
        composable("js_file_picker") {
            PageWrapper {
                JsFilePickerScreen(onFileSelected = { file ->
                    persistence.saveActiveSourcePath(file.absolutePath)
                    navController.popBackStack("source_management", inclusive = false)
                })
            }
        }
        composable("local_music_scan_menu") {
            PageWrapper {
                LocalMusicScanMenuScreen(
                    currentPath = selectedFolderPath,
                    onCustomPathClick = { navController.navigate("folder_picker") },
                    onScanClick = { 
                        scanTriggerCount++
                        navController.popBackStack("player", inclusive = false)
                    }
                )
            }
        }
        composable("folder_picker") {
            PageWrapper {
                FolderPickerScreen(onFolderSelected = { folder ->
                    selectedFolderPath = folder.absolutePath
                    navController.popBackStack()
                })
            }
        }
        composable("lyrics_settings") {
            PageWrapper {
                LyricsSettingsScreen(
                    persistence = persistence,
                    detectEnabled = detectEmbeddedLyrics,
                    onToggleDetect = { detectEmbeddedLyrics = it }
                )
            }
        }
        composable("output_device_picker") {
            PageWrapper {
                OutputDevicePickerScreen(
                    controller = controller,
                    volumeViewModel = volumeViewModel,
                    onConnectClick = {
                        appContext.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    },
                    onThisWatchClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun MainPagerScreen(
    navController: NavHostController,
    controller: MediaController?, 
    playerViewModel: PlayerViewModel?,
    volumeViewModel: VolumeViewModel,
    mainActivityViewModel: MainActivityViewModel? = null,
    onOutputClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onTimerClick: () -> Unit,
    remainingTimeFlow: StateFlow<Long?>,
    customPath: String?,
    scanTrigger: Int,
    lastHandledScanTrigger: Int,
    onScanHandled: (Int) -> Unit,
    onPathApplied: (String) -> Unit,
    persistence: PlaybackPersistence,
    detectLyrics: Boolean,
    backgroundBlur: Boolean,
    verticalPagerState: androidx.compose.foundation.pager.PagerState,
    horizontalPagerState: androidx.compose.foundation.pager.PagerState
) {
    val appContext = LocalContext.current
    val repository = remember { (appContext.applicationContext as com.example.lxn2.LXNApplication).container.mediaRepository }

    // 启动时不再自动请求权限或跳转系统设置，仅以当前权限状态作为扫描开启条件。
    var hasPermission by remember {
        mutableStateOf(PermissionManager.canAccessMusicStorage())
    }

    var isScanning by remember { mutableStateOf(false) }
    var scannedCount by remember { mutableIntStateOf(0) }
    var totalCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(hasPermission, scanTrigger, controller) {
        if (hasPermission && controller != null && customPath != null) {
            val isManualTrigger = scanTrigger > lastHandledScanTrigger
            if (isManualTrigger) {
                onScanHandled(scanTrigger)
                isScanning = true
                scannedCount = 0
                totalCount = 0
                val musicList = withContext(Dispatchers.IO) {
                    repository.getMusicWithProgress(customPath) { scanned, total ->
                        scannedCount = scanned
                        totalCount = total
                    }
                }
                if (musicList.isNotEmpty()) {
                    controller.setMediaItems(musicList)
                    val lastIdZero = persistence.getLastPlayedIdFromZero()
                    val index = musicList.indexOfFirst { it.mediaId == lastIdZero }
                    if (index != -1) controller.seekTo(index, 0L) else controller.seekTo(0, 0L)
                    controller.prepare()
                }
                onPathApplied(customPath)
                isScanning = false
            }
        }
    }

    // 移除了重复的背景渲染逻辑，现在由 MainActivity 的全局背景负责。
    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = verticalPagerState,
            modifier = Modifier.fillMaxSize()
        ) { vPage ->
            // 移除这里的 .onRotaryScrollEvent { true } 包装，改由内部组件自消耗
            when (vPage) {
                0 -> {
                    HorizontalPager(
                        state = horizontalPagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { hPage ->
                        // 移除这里的 .onRotaryScrollEvent { true } 包装
                        when (hPage) {
                            0 -> PlayerUI(playerViewModel, volumeViewModel, controller, onOutputClick)
                            1 -> LyricsScreenContent(controller, detectLyrics, persistence)
                        }
                    }
                }
                1 -> {
                    MoreScreen(
                        onLibraryClick = onLibraryClick,
                        onPlayListClick = { navController.navigate("playlist") },
                        onSearchClick = { navController.navigate("search") },
                        onSettingsClick = { navController.navigate("settings") },
                        onTimerClick = onTimerClick,
                        remainingTimeFlow = remainingTimeFlow,
                        isFocused = verticalPagerState.currentPage == 1
                    )
                }
            }
        }
        if (isScanning) ScanningOverlay(scannedCount, totalCount)
    }
}
