@file:OptIn(ExperimentalHorologistApi::class)
package com.example.lxn2.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.SwitchButtonDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.lxn2.data.LyricLine
import com.example.lxn2.data.MediaRepository
import com.example.lxn2.data.PlaybackPersistence
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import com.example.lxn2.presentation.theme.LXNTheme
import com.example.lxn2.presentation.theme.ThemeColors
import com.example.lxn2.presentation.viewmodel.VolumeViewModelFactory
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.audio.ui.VolumePositionIndicator
import com.google.android.horologist.composables.MarqueeText
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import com.google.android.horologist.compose.layout.ScreenScaffold as HorologistScreenScaffold
import com.google.android.horologist.audio.ui.volumeRotaryBehavior
import com.google.android.horologist.images.coil.CoilPaintable
import com.google.android.horologist.media.ui.components.background.ArtworkColorBackground
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.state.PlayerViewModel
import com.google.android.horologist.media.ui.state.PlayerUiState
import com.google.android.horologist.media.ui.state.model.MediaUiModel
import com.google.android.horologist.media.ui.state.model.TrackPositionUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import okhttp3.OkHttpClient
import okhttp3.Request

@OptIn(ExperimentalHorologistApi::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen() // 安装启动页并获取实例
        super.onCreate(savedInstanceState)
        setContent {
            val activityViewModel: MainActivityViewModel = viewModel()
            val mediaController by activityViewModel.mediaController.collectAsStateWithLifecycle()
            val playerRepository by activityViewModel.playerRepository.collectAsStateWithLifecycle()
            val isReady by activityViewModel.isReady.collectAsStateWithLifecycle()

            // 保持启动页直到 ViewModel 准备就绪
            splashScreen.setKeepOnScreenCondition { !isReady }

            val context = LocalContext.current
            val persistence = remember { PlaybackPersistence(context) }
            var currentThemeColor by remember { mutableStateOf(Color(persistence.getThemeColor())) }

            LXNTheme(primaryColor = currentThemeColor) {
                AppScaffold {
                    val navController = rememberSwipeDismissableNavController()
                    
                    val playerViewModel: PlayerViewModel? = remember(playerRepository) {
                        playerRepository?.let { PlayerViewModel(it) }
                    }
                    val volumeViewModel: VolumeViewModel = viewModel(
                        factory = VolumeViewModelFactory(this@MainActivity)
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
                        onThemeColorChange = { color ->
                            currentThemeColor = color
                            persistence.saveThemeColor(color.toArgb().toLong())
                        }
                    )
                }
            }
        }
    }
}

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
    val context = LocalContext.current
    val persistence = remember { PlaybackPersistence(context) }
    
    var selectedFolderPath by rememberSaveable { mutableStateOf(persistence.getLastPath()) }
    var scanTriggerCount by rememberSaveable { mutableIntStateOf(0) }
    var lastAppliedPath by remember { mutableStateOf<String?>(null) }
    var lastHandledScanTrigger by rememberSaveable { mutableIntStateOf(0) }
    var detectEmbeddedLyrics by rememberSaveable { mutableStateOf(true) }
    var backgroundBlur by rememberSaveable { mutableStateOf(persistence.isBackgroundBlurEnabled()) }

    val verticalPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val horizontalPagerState = rememberPagerState(pageCount = { 2 })
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
                    // 点击歌曲播放后，手动重置 Pager 并返回
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
                    // 点击歌曲播放后，手动重置 Pager 并返回
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
                    (context as Activity).startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
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
    val context = LocalContext.current
    val repository = remember { MediaRepository(context) }

    // 只有在明确需要时（如点击播放后）才执行归位逻辑
    // 这里我们不再使用全局监听，而是由各个操作触发
    
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
                data = "package:${context.packageName}".toUri()
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

    ScreenScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            if (backgroundBlur) {
                ArtworkColorBackground(
                    paintable = currentMediaItem?.mediaMetadata?.artworkUri?.let { CoilPaintable(it) },
                    defaultColor = MaterialTheme.colorScheme.primaryContainer
                )
            } else {
                // 直接显示歌曲封面
                AsyncImage(
                    model = currentMediaItem?.mediaMetadata?.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // 添加一层极淡的遮罩以保证文字可读性
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
                                        if (dragAmount > 25) { // 检测向右滑动
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
                                remainingTimeFlow = remainingTimeFlow
                            )
                        }
                    }
                }
            }
            if (isScanning) ScanningOverlay(scannedCount, totalCount)
        }
    }
}

@Composable
fun ScanningOverlay(scanned: Int, total: Int) {
    val progress = if (total > 0) (scanned.toFloat() / total * 100).roundToInt() else 0
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$progress%", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "$scanned/$total", style = MaterialTheme.typography.titleMedium, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CustomMediaControlButtons(
    playing: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    trackPositionUiModel: TrackPositionUiModel
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "上一首",
                tint = MaterialTheme.colorScheme.primary, // 同步主题色
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center Play/Pause with Progress Ring
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
            // Background Ring
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                colors = ProgressIndicatorDefaults.colors(
                    indicatorColor = Color.White.copy(alpha = 0.1f),
                    trackColor = Color.Transparent
                ),
                strokeWidth = 3.dp
            )

            // Dynamic Progress
            if (trackPositionUiModel is TrackPositionUiModel.Actual) {
                CircularProgressIndicator(
                    progress = { trackPositionUiModel.percent },
                    modifier = Modifier.fillMaxSize(),
                    colors = ProgressIndicatorDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primary, // 同步主题色
                        trackColor = Color.Transparent
                    ),
                    strokeWidth = 3.dp
                )
            }

            // Central Play/Pause Icon
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "播放/暂停" else "播放/暂停",
                    tint = MaterialTheme.colorScheme.primary, // 同步主题色
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Next Button
        IconButton(
            onClick = onNextClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "下一首",
                tint = MaterialTheme.colorScheme.primary, // 同步主题色
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun PlayerUI(
    playerViewModel: PlayerViewModel?,
    volumeViewModel: VolumeViewModel,
    controller: MediaController?,
    onOutputClick: () -> Unit
) {
    val playerUiState by (playerViewModel?.playerUiState ?: remember { MutableStateFlow(PlayerUiState.NotConnected) }).collectAsStateWithLifecycle()
    val volumeUiState by volumeViewModel.volumeUiState.collectAsStateWithLifecycle()

    var repeatMode by remember(controller) { mutableIntStateOf(controller?.repeatMode ?: Player.REPEAT_MODE_ALL) }
    var shuffleEnabled by remember(controller) { mutableStateOf(controller?.shuffleModeEnabled ?: false) }

    var fallbackTitle by remember { mutableStateOf<String?>(null) }
    var fallbackArtist by remember { mutableStateOf<String?>(null) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_TIMELINE_CHANGED)) {
                    fallbackTitle = player.currentMediaItem?.mediaMetadata?.title?.toString()
                    fallbackArtist = player.currentMediaItem?.mediaMetadata?.artist?.toString()
                }
                if (events.contains(Player.EVENT_REPEAT_MODE_CHANGED)) {
                    repeatMode = player.repeatMode
                    Log.d("LXN", "RepeatMode changed: ${player.repeatMode}")
                }
                if (events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
                    shuffleEnabled = player.shuffleModeEnabled
                    Log.d("LXN", "ShuffleMode changed: ${player.shuffleModeEnabled}")
                }
            }

            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }

            override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                shuffleEnabled = enabled
            }
        }
        controller?.addListener(listener)
        fallbackTitle = controller?.currentMediaItem?.mediaMetadata?.title?.toString()
        fallbackArtist = controller?.currentMediaItem?.mediaMetadata?.artist?.toString()
        repeatMode = controller?.repeatMode ?: Player.REPEAT_MODE_ALL
        shuffleEnabled = controller?.shuffleModeEnabled ?: false
        onDispose { controller?.removeListener(listener) }
    }

    val readyMedia = playerUiState.media as? MediaUiModel.Ready
    val displayTitle = readyMedia?.title ?: fallbackTitle ?: "无歌曲播放"
    val displayArtist = readyMedia?.subtitle ?: fallbackArtist ?: "请扫描本地音乐"

    ScreenScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            PlayerScreen(
                mediaDisplay = { SlowerMarqueeMediaDisplay(displayTitle, displayArtist, Modifier.fillMaxWidth().padding(top = 10.dp)) },
                controlButtons = {
                    CustomMediaControlButtons(
                        playing = playerUiState.playing,
                        onPlayPauseClick = {
                            if (playerUiState.playing) playerViewModel?.playerUiController?.pause()
                            else playerViewModel?.playerUiController?.play()
                        },
                        onPreviousClick = { playerViewModel?.playerUiController?.skipToPreviousMedia() },
                        onNextClick = { playerViewModel?.playerUiController?.skipToNextMedia() },
                        trackPositionUiModel = playerUiState.trackPositionUiModel
                    )
                },
                buttons = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOutputClick,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "音量",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = {
                                controller?.let {
                                    Log.d("LXN", "Mode Click: shuffle=$shuffleEnabled, repeat=$repeatMode")
                                    when {
                                        shuffleEnabled -> {
                                            it.repeatMode = Player.REPEAT_MODE_ALL
                                            it.shuffleModeEnabled = false
                                        }
                                        repeatMode == Player.REPEAT_MODE_ONE -> {
                                            it.repeatMode = Player.REPEAT_MODE_ALL
                                            it.shuffleModeEnabled = true
                                        }
                                        else -> {
                                            it.repeatMode = Player.REPEAT_MODE_ONE
                                            it.shuffleModeEnabled = false
                                        }
                                    }
                                }
                            }
                        ) {
                            val icon = when {
                                shuffleEnabled -> Icons.Default.Shuffle
                                repeatMode == Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            }
                            Icon(icon, contentDescription = "播放模式")
                        }
                    }
                },
                modifier = Modifier.requestFocusOnHierarchyActive().rotaryScrollable(
                    volumeRotaryBehavior(volumeUiStateProvider = { volumeUiState }, onRotaryVolumeInput = { newVolume -> volumeViewModel.setVolume(newVolume) }),
                    focusRequester = remember { FocusRequester() }
                )
            )
            VolumePositionIndicator(
                volumeUiState = { volumeUiState },
                displayIndicatorEvents = volumeViewModel.displayIndicatorEvents,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun SlowerMarqueeMediaDisplay(title: String?, artist: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AnimatedContent(targetState = title, transitionSpec = { (slideInHorizontally(tween(60)) { (it * 0.125f).roundToInt() } + fadeIn(tween(60))) togetherWith (slideOutHorizontally { (-it * 0.125f).roundToInt() } + fadeOut()) }, label = "") { currentTitle ->
            MarqueeText(
                text = currentTitle.orEmpty(), 
                modifier = Modifier.padding(horizontal = 12.dp), 
                color = MaterialTheme.colorScheme.onBackground, 
                style = MaterialTheme.typography.titleMedium.copy(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(3f, 3f), blurRadius = 8f)
                ), 
                textAlign = TextAlign.Center, 
                marqueeDpPerSecond = 15.dp, 
                edgeGradientWidth = 30.dp // 修复：主页面亮度低和卡顿问题
            )
        }
        AnimatedContent(targetState = artist, transitionSpec = { (slideInHorizontally(tween(90)) { (it * 0.125f).roundToInt() } + fadeIn(tween(90))) togetherWith (slideOutHorizontally(tween(30)) { (-it * 0.125f).roundToInt() } + fadeOut(tween(30))) }, label = "") { currentArtist ->
            Text(text = currentArtist.orEmpty(), modifier = Modifier.fillMaxWidth().padding(top = 1.dp), color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun LyricsScreenContent(controller: MediaController?, detectLyrics: Boolean) {
    val context = LocalContext.current
    val repository = remember { MediaRepository(context) }
    var lyricLines by remember { mutableStateOf(emptyList<LyricLine>()) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    
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

    LaunchedEffect(currentMediaItem, detectLyrics) {
        lyricLines = if (currentMediaItem != null && detectLyrics) {
            repository.getParsedLyrics(currentMediaItem!!)
        } else {
            emptyList()
        }
    }

    LaunchedEffect(controller) { while (true) { currentPosition = controller?.currentPosition ?: 0L; delay(100.milliseconds) } }

    val activeIndex = remember(lyricLines, currentPosition) { 
        val lastMatchingIdx = lyricLines.indexOfLast { it.timeMs <= currentPosition }
        if (lastMatchingIdx == -1) return@remember -1
        val activeTime = lyricLines[lastMatchingIdx].timeMs
        lyricLines.indexOfFirst { it.timeMs == activeTime }
    }
    
    val columnState = remember { ScalingLazyColumnState() }
    LaunchedEffect(activeIndex) { if (activeIndex >= 0) columnState.state.animateScrollToItem(activeIndex + 1) }

    HorologistScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = columnState.state, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 50.dp)) {
            item { ListHeader { Text("歌词") } }
            if (lyricLines.isEmpty()) {
                item { Text(text = if (detectLyrics) "暂无歌词" else "歌词功能已禁用", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            } else {
                items(lyricLines) { line: LyricLine ->
                    val isActive = line == lyricLines.getOrNull(activeIndex)
                    val alpha by animateFloatAsState(if (isActive) 1f else 0.5f)
                    Text(
                        text = line.text, 
                        textAlign = TextAlign.Center, 
                        fontSize = if (isActive) 16.sp else 14.sp, 
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, 
                        color = if (isActive) Color.White else Color.LightGray, 
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(alpha)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun MoreScreen(
    onLibraryClick: () -> Unit,
    onPlayListClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTimerClick: () -> Unit,
    remainingTimeFlow: StateFlow<Long?>
) {
    val remainingTime by remainingTimeFlow.collectAsStateWithLifecycle()
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = columnState,
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 30.dp, top = 26.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            // Home Icon and Quick Buttons at the top
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home, // 圆润的房子图标
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onSearchClick,
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp))
                            }
                        }
                        FilledTonalButton(
                            onClick = onLibraryClick, // 点击进入库
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LibraryMusic, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            item { 
                FilledTonalButton(
                    onClick = onPlayListClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("播放列表")
                } 
            }
            item { 
                FilledTonalButton(
                    onClick = onTimerClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Timer, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    val currentTime = remainingTime
                    val timerText = if (currentTime != null) {
                        val h = currentTime / 3600
                        val m = (currentTime % 3600) / 60
                        val s = currentTime % 60
                        if (h > 0) {
                            "定时中: $h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
                        } else {
                            "定时中: ${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
                        }
                    } else "定时暂停"
                    Text(timerText)
                } 
            }
            item { 
                FilledTonalButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("更多设置")
                } 
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun PlaylistScreen(controller: MediaController?, persistence: PlaybackPersistence, onDismiss: () -> Unit) {
    val columnState = rememberScalingLazyListState()
    var mediaItems by remember { mutableStateOf(emptyList<MediaItem>()) }
    fun updateList() {
        val items = mutableListOf<MediaItem>()
        controller?.let { for (i in 0 until it.mediaItemCount) items.add(it.getMediaItemAt(i)) }
        mediaItems = items
    }
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_PLAYLIST_METADATA_CHANGED)) updateList()
            }
        }
        controller?.addListener(listener)
        updateList()
        onDispose { controller?.removeListener(listener) }
    }
    val history = remember { persistence.getPlaybackHistory() }
    val currentMediaId = controller?.currentMediaItem?.mediaId
    val sortedItems = remember(mediaItems, history) {
        mediaItems.distinctBy { it.mediaId }.sortedWith { a, b ->
            val indexA = history.indexOf(a.mediaId)
            val indexB = history.indexOf(b.mediaId)
            when {
                indexA != -1 && indexB != -1 -> indexA.compareTo(indexB)
                indexA != -1 -> -1
                indexB != -1 -> 1
                else -> (a.mediaMetadata.title?.toString() ?: "").compareTo(b.mediaMetadata.title?.toString() ?: "")
            }
        }
    }
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { ListHeader { Text("播放列表") } }
            if (sortedItems.isEmpty()) {
                item { Text(text = "列表为空，请先扫描音乐", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            } else {
                items(sortedItems) { item: MediaItem ->
                    val isCurrent = item.mediaId == currentMediaId
                    Button(
                        onClick = {
                            controller?.let {
                                for (i in 0 until it.mediaItemCount) {
                                    if (it.getMediaItemAt(i).mediaId == item.mediaId) { it.seekTo(i, 0L); it.play(); onDismiss(); break }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (isCurrent) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                MarqueeText(
                                    text = item.mediaMetadata.title?.toString() ?: "未知曲目",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    marqueeDpPerSecond = 10.dp
                                )
                                MarqueeText(
                                    text = item.mediaMetadata.artist?.toString() ?: "未知歌手",
                                    style = MaterialTheme.typography.labelSmall,
                                    marqueeDpPerSecond = 8.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            val origin = item.mediaMetadata.extras?.getString("origin") ?: "LOCAL"
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = origin,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(controller: MediaController?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val columnState = rememberScalingLazyListState()
    val repository = remember { MediaRepository(context) }
    
    var keyword by remember { mutableStateOf("") }
    var onlineResults by remember { mutableStateOf(emptyList<MediaItem>()) }
    var localResults by remember { mutableStateOf(emptyList<MediaItem>()) }
    var isSearching by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val res = android.app.RemoteInput.getResultsFromIntent(result.data)
            res?.getCharSequence("search_keyword")?.toString()?.let { keyword = it }
        }
    }

    LaunchedEffect(keyword) {
        if (keyword.isNotEmpty()) {
            isSearching = true
            onlineResults = emptyList()
            
            // Local Search
            withContext(Dispatchers.IO) {
                val allLocal = repository.getCachedMusic()
                localResults = allLocal.filter {
                    (it.mediaMetadata.title?.toString()?.contains(keyword, ignoreCase = true) == true) ||
                    (it.mediaMetadata.artist?.toString()?.contains(keyword, ignoreCase = true) == true)
                }
            }

            // Online Search
            val client = OkHttpClient()
            withContext(Dispatchers.IO) {
                try {
                    val encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8")
                    val searchUrl = "http://search.kuwo.cn/r.s?client=kt&all=$encodedKeyword&pn=0&rn=20&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1&show_copyright_off=1&newver=1&ft=music&encoding=utf8&rformat=json&mobi=1"
                    
                    val request = Request.Builder().url(searchUrl).build()
                    val resp = client.newCall(request).execute().body?.string()
                    
                    val cleanJson = resp?.replace("'", "\"") ?: "{}"
                    val json = JSONObject(cleanJson)
                    val songs = json.optJSONArray("abslist")
                    val list = mutableListOf<MediaItem>()
                    if (songs != null) {
                        for (i in 0 until songs.length()) {
                            val s = songs.getJSONObject(i)
                            val songId = s.optString("MUSICRID").replace("MUSIC_", "")
                            val artist = s.optString("ARTIST")
                            val title = s.optString("SONGNAME")
                            val metadata = MediaMetadata.Builder()
                                .setTitle(title)
                                .setArtist(artist)
                                .setExtras(Bundle().apply { putString("origin", "ONLINE") })
                                .build()
                            
                            val playUrl = "http://antiserver.kuwo.cn/anti.s?format=mp3&rid=MUSIC_$songId&type=convert_url&response=url&bitrate=128"
                            
                            list.add(
                                MediaItem.Builder()
                                    .setMediaId(songId)
                                    .setMediaMetadata(metadata)
                                    .setUri(playUrl.toUri())
                                    .build()
                            )
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onlineResults = list
                    }
                } catch (e: Exception) { 
                    Log.e("LXN", "Search Failed", e) 
                    try {
                        val searchUrl = "https://music.163.com/api/search/get/web?s=$keyword&type=1&limit=20"
                        val request = Request.Builder().url(searchUrl).build()
                        val resp = client.newCall(request).execute().body?.string()
                        val json = JSONObject(resp ?: "{}")
                        val songs = json.optJSONObject("result")?.optJSONArray("songs")
                        val list = mutableListOf<MediaItem>()
                        if (songs != null) {
                            for (i in 0 until songs.length()) {
                                val s = songs.getJSONObject(i)
                                val songId = s.optString("id")
                                val artist = s.optJSONArray("artists")?.optJSONObject(0)?.optString("name") ?: "Unknown"
                                val metadata = MediaMetadata.Builder()
                                    .setTitle(s.optString("name"))
                                    .setArtist(artist)
                                    .setExtras(Bundle().apply { putString("origin", "ONLINE") })
                                    .build()
                                list.add(
                                    MediaItem.Builder()
                                        .setMediaId(songId)
                                        .setMediaMetadata(metadata)
                                    .setUri("https://music.163.com/song/media/outer/url?id=$songId.mp3".toUri())
                                    .build()
                                )
                            }
                        }
                        withContext(Dispatchers.Main) { onlineResults = list }
                    } catch (e2: Exception) { Log.e("LXN", "Fallback Search Failed", e2) }
                } finally { 
                    isSearching = false 
                }
            }
        }
    }

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { ListHeader { Text("搜索") } }
            item { 
                Button(
                    onClick = {
                        try {
                            val intent = Intent("android.support.wearable.input.action.REMOTE_INPUT")
                            val remoteInput = android.app.RemoteInput.Builder("search_keyword")
                                .setLabel("输入歌曲或歌手")
                                .build()
                            intent.putExtra("android.support.wearable.input.extra.REMOTE_INPUTS", arrayOf(remoteInput))
                            launcher.launch(intent)
                        } catch (e: Exception) {
                            Log.e("LXN", "Failed to launch remote input", e)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Keyboard, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(keyword.ifEmpty { "点击输入" })
                } 
            }
            if (isSearching) {
                item { 
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator() 
                    }
                }
            } else if (keyword.isNotEmpty() && localResults.isEmpty() && onlineResults.isEmpty()) {
                item {
                    Text(
                        text = "未找到相关歌曲",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Local Results
            if (localResults.isNotEmpty()) {
                item { ListHeader { Text("本地歌曲") } }
                items(localResults) { song: MediaItem ->
                    Button(
                        onClick = {
                            controller?.let {
                                var foundIndex = -1
                                for (i in 0 until it.mediaItemCount) {
                                    if (it.getMediaItemAt(i).mediaId == song.mediaId) { foundIndex = i; break }
                                }
                                if (foundIndex != -1) {
                                    it.seekTo(foundIndex, 0L)
                                } else {
                                    it.addMediaItem(song)
                                    it.seekTo(it.mediaItemCount - 1, 0L)
                                }
                                it.prepare()
                                it.play()
                            }
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Column {
                            Text(song.mediaMetadata.title.toString(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.mediaMetadata.artist.toString(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Online Results
            if (onlineResults.isNotEmpty()) {
                item { ListHeader { Text("在线歌曲") } }
                items(onlineResults) { song: MediaItem ->
                    Button(
                        onClick = {
                            val playApiUrl = song.localConfiguration?.uri?.toString() ?: ""
                            if (playApiUrl.contains("kuwo.cn")) {
                                val client = OkHttpClient()
                                val scope = CoroutineScope(Dispatchers.IO)
                                scope.launch {
                                    try {
                                        val resp = client.newCall(Request.Builder().url(playApiUrl).build()).execute().body?.string()
                                        if (!resp.isNullOrBlank() && resp.startsWith("http")) {
                                            withContext(Dispatchers.Main) {
                                                controller?.let {
                                                    val finalSong = song.buildUpon().setUri(resp.toUri()).build()
                                                    var foundIndex = -1
                                                    for (i in 0 until it.mediaItemCount) {
                                                        if (it.getMediaItemAt(i).mediaId == song.mediaId) { foundIndex = i; break }
                                                    }
                                                    if (foundIndex != -1) {
                                                        it.seekTo(foundIndex, 0L)
                                                    } else {
                                                        it.addMediaItem(finalSong)
                                                        it.seekTo(it.mediaItemCount - 1, 0L)
                                                    }
                                                    it.prepare()
                                                    it.play()
                                                }
                                                onDismiss()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("LXN", "Failed to fetch Kuwo URL", e)
                                    }
                                }
                            } else {
                                controller?.let {
                                    var foundIndex = -1
                                    for (i in 0 until it.mediaItemCount) {
                                        if (it.getMediaItemAt(i).mediaId == song.mediaId) { foundIndex = i; break }
                                    }
                                    if (foundIndex != -1) {
                                        it.seekTo(foundIndex, 0L)
                                    } else {
                                        it.addMediaItem(song)
                                        it.seekTo(it.mediaItemCount - 1, 0L)
                                    }
                                    it.prepare()
                                    it.play()
                                }
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Column {
                            Text(song.mediaMetadata.title.toString(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.mediaMetadata.artist.toString(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun SettingsScreen(
    onScanLocalMusicClick: () -> Unit, 
    onSourceManagementClick: () -> Unit, 
    onLyricsSettingsClick: () -> Unit,
    onThemeSettingsClick: () -> Unit
) {
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { ListHeader { Text("设置") } }
            item { 
                FilledTonalButton(
                    onClick = onScanLocalMusicClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("本地音乐扫描")
                } 
            }
            item { 
                FilledTonalButton(
                    onClick = onSourceManagementClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Extension, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("自定义源管理")
                } 
            }
            item { 
                FilledTonalButton(
                    onClick = { onLyricsSettingsClick() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lyrics, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("歌词设置")
                } 
            }
            item { 
                FilledTonalButton(
                    onClick = onThemeSettingsClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Palette, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("应用主题")
                } 
            }
        }
    }
}

@Composable
fun ThemeSettingsScreen(
    blurEnabled: Boolean, 
    onToggleBlur: (Boolean) -> Unit, 
    onThemeColorClick: () -> Unit
) {
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { ListHeader { Text("应用主题") } }
            item {
                SwitchButton(
                    checked = blurEnabled,
                    onCheckedChange = onToggleBlur,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("背景模糊") }
                )
            }
            item {
                FilledTonalButton(
                    onClick = onThemeColorClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Palette, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("主题色")
                }
            }
        }
    }
}

@Composable
fun ThemeColorPickerScreen(onColorSelected: (Color) -> Unit) {
    val columnState = remember { ScalingLazyColumnState() }
    HorologistScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState.state,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("选择颜色") } }
            items(ThemeColors) { themeColor ->
                Button(
                    onClick = { onColorSelected(themeColor.color) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(themeColor.color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(themeColor.name)
                    }
                }
            }
        }
    }
}

@Composable
fun SourceManagementScreen(persistence: PlaybackPersistence, onAddSourceClick: () -> Unit) {
    val columnState = remember { ScalingLazyColumnState() }
    val activeSource = persistence.getActiveSourcePath()
    HorologistScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState.state,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text("源管理") } }
            item {
                Button(
                    onClick = onAddSourceClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("当前源: ${if (activeSource != null) File(activeSource).name else "未设置"}")
                        Text(activeSource ?: "点击选择 .js 文件", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (activeSource != null) {
                item {
                    Button(
                        onClick = { persistence.saveActiveSourcePath(null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("清除当前源")
                    }
                }
            }
        }
    }
}

@Composable
fun JsFilePickerScreen(onFileSelected: (File) -> Unit) {
    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    
    var hasPermission by remember {
        mutableStateOf(Environment.isExternalStorageManager())
    }

    val entries = remember(currentDir, hasPermission) { 
        val list = currentDir.listFiles() ?: emptyArray()
        list.filter { file -> 
            file.isDirectory || file.name.endsWith(".js", ignoreCase = true) 
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    val columnState = remember { ScalingLazyColumnState() }
    HorologistScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState.state,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("选择 JS 脚本") } }
            
            if (!hasPermission) {
                item {
                    Text("请在系统设置中授予\"所有文件访问权限\"", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }

            if (currentDir != Environment.getExternalStorageDirectory()) { 
                item { 
                    Button(
                        onClick = { currentDir = currentDir.parentFile ?: currentDir }, 
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ArrowUpward, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("返回")
                    } 
                } 
            }
            
            items(entries) { file: File -> 
                Button(
                    onClick = { if (file.isDirectory) currentDir = file else onFileSelected(file) }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = Color.White)
                ) {
                    Icon(if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } 
            }
        }
    }
}

@Composable
fun LocalMusicScanMenuScreen(currentPath: String?, onCustomPathClick: () -> Unit, onScanClick: () -> Unit) {
    val columnState = remember { ScalingLazyColumnState() }
    HorologistScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState.state,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text("本地音乐扫描") } }
            item { 
                Button(
                    onClick = onCustomPathClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("自定义扫描目录")
                        Text(currentPath ?: "未选择", style = MaterialTheme.typography.bodySmall)
                    }
                } 
            }
            item { 
                Button(
                    onClick = { onScanClick() },
                    enabled = currentPath != null, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("加载音乐文件")
                } 
            }
        }
    }
}

@Composable
fun LyricsSettingsScreen(detectEnabled: Boolean, onToggleDetect: (Boolean) -> Unit) {
    val columnState = remember { ScalingLazyColumnState() }
    HorologistScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState.state,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text("歌词设置") } }
            item { 
                SwitchButton(
                    checked = detectEnabled, 
                    onCheckedChange = onToggleDetect, 
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("检测歌曲内嵌歌词") },
                    colors = SwitchButtonDefaults.switchButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        uncheckedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        checkedContentColor = Color.White,
                        uncheckedContentColor = Color.White,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                ) 
            }
        }
    }
}

@Composable
fun OutputDevicePickerScreen(controller: MediaController?, onConnectClick: () -> Unit, onThisWatchClick: () -> Unit) {
    val columnState = remember { ScalingLazyColumnState() }
    var playerVolume by remember(controller) { mutableFloatStateOf(controller?.volume ?: 1f) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onVolumeChanged(volume: Float) {
                playerVolume = volume
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    HorologistScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState.state,
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 30.dp, top = 20.dp), // 缩小顶部多余空间
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { ListHeader { Text("播放控件", color = MaterialTheme.colorScheme.onBackground) } }
            
            // Volume Control Pill
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { controller?.let { it.volume = (it.volume - 0.05f).coerceAtLeast(0f) } },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.onSurface)
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(playerVolume)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

                    IconButton(
                        onClick = { controller?.let { it.volume = (it.volume + 0.05f).coerceAtMost(1f) } },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "连接设备",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onThisWatchClick)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "这只手表",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

fun Modifier.fadingEdge() = this
    .graphicsLayer { alpha = 0.99f }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color.Transparent,
                0.1f to Color.Black,
                0.9f to Color.Black,
                1.0f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

@Composable
fun LibraryScreen(onLocalMusicClick: () -> Unit, onArtistsClick: () -> Unit, onAlbumsClick: () -> Unit) {
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { ListHeader { Text("库") } }
            
            item {
                FilledTonalButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("下载")
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = onLocalMusicClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("歌单")
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = onArtistsClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("艺人")
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = onAlbumsClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Album, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("专辑")
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistsScreen(onArtistClick: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { MediaRepository(context) }
    val artists = remember { repository.getArtists() }
    val columnState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { ListHeader { Text("艺人") } }
            if (artists.isEmpty()) {
                item { Text("暂无艺人信息", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            } else {
                items(artists) { artist ->
                    FilledTonalButton(
                        onClick = { onArtistClick(artist) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumsScreen(onAlbumClick: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { MediaRepository(context) }
    val albums = remember { repository.getAlbums() }
    val columnState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { ListHeader { Text("专辑") } }
            if (albums.isEmpty()) {
                item { Text("暂无专辑信息", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            } else {
                items(albums) { album ->
                    FilledTonalButton(
                        onClick = { onAlbumClick(album) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(album, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun FilteredMusicScreen(title: String, filterType: String, filterValue: String, controller: MediaController?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { MediaRepository(context) }
    val musicItems = remember {
        if (filterType == "artist") repository.getMusicByArtist(filterValue)
        else repository.getMusicByAlbum(filterValue)
    }
    val columnState = rememberScalingLazyListState()
    val currentMediaId = controller?.currentMediaItem?.mediaId

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { ListHeader { Text(title) } }
            items(musicItems) { item ->
                val isCurrent = item.mediaId == currentMediaId
                Button(
                    onClick = {
                        controller?.let {
                            var foundIndex = -1
                            for (i in 0 until it.mediaItemCount) {
                                if (it.getMediaItemAt(i).mediaId == item.mediaId) { foundIndex = i; break }
                            }
                            if (foundIndex != -1) {
                                it.seekTo(foundIndex, 0L)
                            } else {
                                it.addMediaItem(item)
                                it.seekTo(it.mediaItemCount - 1, 0L)
                            }
                            it.prepare()
                            it.play()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isCurrent) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            MarqueeText(
                                text = item.mediaMetadata.title?.toString() ?: "未知曲目",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                marqueeDpPerSecond = 10.dp
                            )
                        }
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerSetupScreen(
    initialHours: Int,
    initialMinutes: Int,
    initialSeconds: Int,
    onTimeClick: () -> Unit,
    onConfirm: (Int, Boolean) -> Unit, 
    onCancel: () -> Unit,
    navController: NavHostController
) {
    var timerEnabled by remember { mutableStateOf(true) }
    var finishSong by remember { mutableStateOf(false) }
    
    // 从导航结果中恢复设置的时间
    val navEntry = navController.currentBackStackEntry
    val h = navEntry?.savedStateHandle?.get<Int>("h") ?: initialHours
    val m = navEntry?.savedStateHandle?.get<Int>("m") ?: initialMinutes
    val s = navEntry?.savedStateHandle?.get<Int>("s") ?: initialSeconds

    val columnState = rememberScalingLazyListState()

    HorologistScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 30.dp, top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 已删除 ListHeader "基本设置"

            item {
                SwitchButton(
                    checked = timerEnabled,
                    onCheckedChange = { timerEnabled = it },
                    label = { Text("启用定时", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SwitchButtonDefaults.switchButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        uncheckedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        checkedContentColor = Color.White,
                        uncheckedContentColor = Color.White,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            item {
                SwitchButton(
                    checked = finishSong,
                    onCheckedChange = { finishSong = it },
                    enabled = timerEnabled,
                    label = { Text("播放完整歌曲", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SwitchButtonDefaults.switchButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        uncheckedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        checkedContentColor = Color.White,
                        uncheckedContentColor = Color.White,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (timerEnabled) {
                item {
                    Button(
                        onClick = onTimeClick,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "设定时长", 
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White // 修复对比度
                            )
                            Text(
                                text = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}",
                                color = Color(0xFFADCFFF),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                IconButton(
                    onClick = { 
                        if (timerEnabled) {
                            onConfirm(h * 3600 + m * 60 + s, finishSong)
                        } else {
                            onCancel()
                        }
                    },
                    modifier = Modifier.size(52.dp).background(Color(0xFFADCFFF), CircleShape)
                ) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF003355), modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun TimePickerScreen(onConfirm: (Int, Int, Int) -> Unit) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(5) }
    var seconds by remember { mutableIntStateOf(0) }
    
    // 0: Hours, 1: Minutes, 2: Seconds
    var focusedColumn by remember { mutableIntStateOf(1) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = when(focusedColumn) {
                    0 -> "小时"
                    1 -> "分钟"
                    else -> "秒"
                },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                TimerPickerColumn(
                    initialValue = hours,
                    range = 0..23,
                    isFocused = focusedColumn == 0,
                    onValueChange = { hours = it },
                    onFocus = { focusedColumn = 0 }
                )
                Text(":", style = MaterialTheme.typography.displayMedium, color = Color.Gray)
                TimerPickerColumn(
                    initialValue = minutes,
                    range = 0..59,
                    isFocused = focusedColumn == 1,
                    onValueChange = { minutes = it },
                    onFocus = { focusedColumn = 1 }
                )
                Text(":", style = MaterialTheme.typography.displayMedium, color = Color.Gray)
                TimerPickerColumn(
                    initialValue = seconds,
                    range = 0..59,
                    isFocused = focusedColumn == 2,
                    onValueChange = { seconds = it },
                    onFocus = { focusedColumn = 2 }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            IconButton(
                onClick = { onConfirm(hours, minutes, seconds) },
                modifier = Modifier.size(52.dp).background(Color(0xFFADCFFF), CircleShape)
            ) {
                Icon(Icons.Default.Check, null, tint = Color(0xFF003355), modifier = Modifier.size(28.dp))
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun TimerPickerColumn(
    initialValue: Int,
    range: IntRange,
    isFocused: Boolean,
    onValueChange: (Int) -> Unit,
    onFocus: () -> Unit
) {
    val state = rememberScalingLazyListState(initialCenterItemIndex = initialValue)
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(state.centerItemIndex) {
        onValueChange(state.centerItemIndex)
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .width(50.dp)
            .height(90.dp)
            .onRotaryScrollEvent { false }
            .rotaryScrollable(
                RotaryScrollableDefaults.behavior(state),
                focusRequester = focusRequester
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, _ -> onFocus() }
            }
            .clickable { onFocus() }
    ) {
        ScalingLazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            items(range.toList()) { i ->
                val isSelected = i == state.centerItemIndex
                Text(
                    text = i.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp
                    ),
                    color = if (isFocused && isSelected) Color.White else if (isSelected) Color(0xFFADCFFF) else Color.DarkGray,
                    modifier = Modifier.alpha(if (isSelected) 1f else 0.4f)
                )
            }
        }
    }
}

@Composable
fun FolderPickerScreen(onFolderSelected: (File) -> Unit) {
    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    val files = remember(currentDir) { currentDir.listFiles { file -> file.isDirectory }?.toList()?.sortedBy { it.name } ?: emptyList() }
    val columnState = remember { ScalingLazyColumnState() }
    HorologistScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState.state,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("选择目录") } }
            item { 
                Button(
                    onClick = { onFolderSelected(currentDir) }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("选择该文件夹")
                } 
            }
            if (currentDir != Environment.getExternalStorageDirectory()) { 
                item { 
                    Button(
                        onClick = { currentDir = currentDir.parentFile ?: currentDir }, 
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ArrowUpward, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("返回")
                    } 
                } 
            }
            item { Text(text = currentDir.absolutePath, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
            items(files) { file: File -> 
                Button(
                    onClick = { currentDir = file }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(file.name)
                } 
            }
        }
    }
}
