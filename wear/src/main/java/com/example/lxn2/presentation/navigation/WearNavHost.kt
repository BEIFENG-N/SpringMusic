@file:OptIn(ExperimentalHorologistApi::class)
package com.example.lxn2.presentation.navigation

import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
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
import com.example.lxn2.presentation.library.AlbumsScreen
import com.example.lxn2.presentation.library.ArtistsScreen
import com.example.lxn2.presentation.library.FilteredMusicScreen
import com.example.lxn2.presentation.library.LibraryScreen
import com.example.lxn2.presentation.library.PlaylistScreen
import com.example.lxn2.presentation.library.SearchScreen
import com.example.lxn2.presentation.player.LyricsScreenContent
import com.example.lxn2.presentation.player.OutputDevicePickerScreen
import com.example.lxn2.presentation.player.PlayerUI
import com.example.lxn2.presentation.settings.LyricsSettingsScreen
import com.example.lxn2.presentation.settings.MoreScreen
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

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun WearNavHost(
    navController: NavHostController, 
    controller: MediaController?,
    playerViewModel: PlayerViewModel?,
    volumeViewModel: VolumeViewModel,
    onStartTimer: (Int, Boolean) -> Unit,
    onCancelTimer: () -> Unit,
    remainingTimeFlow: StateFlow<Long?>,
    onThemeColorChange: (Color) -> Unit
) {
    val appContext = LocalContext.current
    val persistence = remember { PlaybackPersistence(appContext) }
    
    var selectedFolderPath by rememberSaveable { mutableStateOf(persistence.getLastPath()) }
    var scanTriggerCount by rememberSaveable { mutableIntStateOf(0) }
    var lastAppliedPath by remember { mutableStateOf<String?>(null) }
    var lastHandledScanTrigger by rememberSaveable { mutableIntStateOf(0) }
    var detectEmbeddedLyrics by rememberSaveable { mutableStateOf(true) }
    var backgroundBlur by rememberSaveable { mutableStateOf(persistence.isBackgroundBlurEnabled()) }

    val verticalPagerState = rememberPagerState(initialPage = 0) { 2 }
    val horizontalPagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "player"
    ) {
        composable("player") {
            MainPagerScreen(
                navController = navController,
                controller = controller, 
                playerViewModel = playerViewModel,
                volumeViewModel = volumeViewModel,
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
        composable("timer_setup") {
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
        composable("time_picker") {
            TimePickerScreen(
                onConfirm = { h, m, s ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("h", h)
                    navController.previousBackStackEntry?.savedStateHandle?.set("m", m)
                    navController.previousBackStackEntry?.savedStateHandle?.set("s", s)
                    navController.popBackStack()
                }
            )
        }
        composable("playlist") {
            PlaylistScreen(
                controller = controller,
                persistence = persistence,
                onDismiss = {
                    scope.launch { verticalPagerState.scrollToPage(0) }
                    navController.popBackStack("player", inclusive = false)
                }
            )
        }
        composable("search") {
            SearchScreen(
                controller = controller,
                onDismiss = { 
                    scope.launch { verticalPagerState.scrollToPage(0) }
                    navController.popBackStack("player", inclusive = false) 
                }
            )
        }
        composable("library") {
            LibraryScreen(
                onLocalMusicClick = { navController.navigate("playlist") },
                onArtistsClick = { navController.navigate("artists") },
                onAlbumsClick = { navController.navigate("albums") }
            )
        }
        composable("artists") {
            ArtistsScreen(
                onArtistClick = { artist -> navController.navigate("artist_songs/$artist") }
            )
        }
        composable(
            route = "artist_songs/{artistName}",
            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
        ) { backStackEntry ->
            val artistName = backStackEntry.arguments?.getString("artistName") ?: ""
            FilteredMusicScreen(
                title = artistName,
                filterType = "artist",
                filterValue = artistName,
                controller = controller,
                onDismiss = { 
                    scope.launch { verticalPagerState.scrollToPage(0) }
                    navController.popBackStack("player", inclusive = false) 
                }
            )
        }
        composable("albums") {
            AlbumsScreen(
                onAlbumClick = { album -> navController.navigate("album_songs/$album") }
            )
        }
        composable(
            route = "album_songs/{albumName}",
            arguments = listOf(navArgument("albumName") { type = NavType.StringType })
        ) { backStackEntry ->
            val albumName = backStackEntry.arguments?.getString("albumName") ?: ""
            FilteredMusicScreen(
                title = albumName,
                filterType = "album",
                filterValue = albumName,
                controller = controller,
                onDismiss = { 
                    scope.launch { verticalPagerState.scrollToPage(0) }
                    navController.popBackStack("player", inclusive = false) 
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onScanLocalMusicClick = { navController.navigate("local_music_scan_menu") },
                onSourceManagementClick = { navController.navigate("source_management") },
                onLyricsSettingsClick = { navController.navigate("lyrics_settings") },
                onThemeSettingsClick = { navController.navigate("theme_settings") }
            )
        }
        composable("theme_settings") {
            ThemeSettingsScreen(
                blurEnabled = backgroundBlur,
                onToggleBlur = { 
                    backgroundBlur = it
                    persistence.saveBackgroundBlur(it)
                },
                onThemeColorClick = { navController.navigate("theme_color_picker") }
            )
        }
        composable("theme_color_picker") {
            ThemeColorPickerScreen(
                onColorSelected = { color ->
                    onThemeColorChange(color)
                    navController.popBackStack()
                }
            )
        }
        composable("source_management") {
            SourceManagementScreen(
                persistence = persistence,
                onAddSourceClick = { navController.navigate("js_file_picker") }
            )
        }
        composable("js_file_picker") {
            JsFilePickerScreen(onFileSelected = { file ->
                persistence.saveActiveSourcePath(file.absolutePath)
                navController.popBackStack("source_management", inclusive = false)
            })
        }
        composable("local_music_scan_menu") {
            LocalMusicScanMenuScreen(
                currentPath = selectedFolderPath,
                onCustomPathClick = { navController.navigate("folder_picker") },
                onScanClick = { 
                    scanTriggerCount++
                    navController.popBackStack("player", inclusive = false)
                }
            )
        }
        composable("folder_picker") {
            FolderPickerScreen(onFolderSelected = { folder ->
                selectedFolderPath = folder.absolutePath
                navController.popBackStack()
            })
        }
        composable("lyrics_settings") {
            LyricsSettingsScreen(
                detectEnabled = detectEmbeddedLyrics,
                onToggleDetect = { detectEmbeddedLyrics = it }
            )
        }
        composable("output_device_picker") {
            OutputDevicePickerScreen(
                controller = controller,
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

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun MainPagerScreen(
    navController: NavHostController,
    controller: MediaController?, 
    playerViewModel: PlayerViewModel?,
    volumeViewModel: VolumeViewModel,
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
    val repository = remember { MediaRepository(appContext) }

    var hasPermission by remember {
        mutableStateOf(Environment.isExternalStorageManager())
    }

    val manageFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = Environment.isExternalStorageManager()
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:${appContext.packageName}".toUri()
            }
            manageFilesLauncher.launch(intent)
        }
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

    var currentMediaItem by remember { mutableStateOf(controller?.currentMediaItem) }
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                   currentMediaItem = player.currentMediaItem
                }
            }
        }
        controller?.addListener(listener)
        currentMediaItem = controller?.currentMediaItem
        onDispose { controller?.removeListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (backgroundBlur) {
            ArtworkColorBackground(
                paintable = currentMediaItem?.mediaMetadata?.artworkUri?.let { CoilPaintable(it) },
                defaultColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            AsyncImage(
                model = currentMediaItem?.mediaMetadata?.artworkUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
        }
        VerticalPager(state = verticalPagerState) { vPage ->
            when (vPage) {
                0 -> {
                    HorizontalPager(state = horizontalPagerState) { hPage ->
                        when (hPage) {
                            0 -> PlayerUI(playerViewModel, volumeViewModel, controller, onOutputClick)
                            1 -> LyricsScreenContent(controller, detectLyrics)
                        }
                    }
                }
                1 -> {
                    val scope = rememberCoroutineScope()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (dragAmount > 25) {
                                        scope.launch {
                                            verticalPagerState.animateScrollToPage(0)
                                        }
                                    }
                                }
                            }
                    ) {
                        MoreScreen(
                            onLibraryClick = onLibraryClick,
                            onPlayListClick = { navController.navigate("playlist") },
                            onSearchClick = { navController.navigate("search") },
                            onSettingsClick = { navController.navigate("settings") },
                            onTimerClick = onTimerClick,
                            remainingTimeFlow = remainingTimeFlow,
                        )
                    }
                }
            }
        }
        if (isScanning) ScanningOverlay(scannedCount, totalCount)
    }
}
