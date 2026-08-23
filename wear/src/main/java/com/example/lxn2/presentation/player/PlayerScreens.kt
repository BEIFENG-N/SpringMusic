package com.example.lxn2.presentation.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.lxn2.data.LyricLine
import com.example.lxn2.data.MediaRepository
import com.example.lxn2.presentation.components.SquigglyBadgeShape
import com.example.lxn2.presentation.components.fadingEdge
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumePositionIndicator
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.audio.ui.volumeRotaryBehavior
import com.google.android.horologist.composables.MarqueeText
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.state.PlayerUiState
import com.google.android.horologist.media.ui.state.PlayerViewModel
import com.google.android.horologist.media.ui.state.model.MediaUiModel
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun CustomMediaControlButtons(
    playing: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 上一首按钮
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 2. 播放/暂停按钮容器
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
            // 核心播放按钮：恢复为之前的 52dp 实色风格
            Box(
                modifier = Modifier
                    .size(52.dp) 
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer) 
                    .clickable { onPlayPauseClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 
                    contentDescription = "播放暂停", 
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer 
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 3. 下一首按钮
        IconButton(
            onClick = onNextClick,
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "下一首", modifier = Modifier.size(24.dp))
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
                }
                if (events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
                    shuffleEnabled = player.shuffleModeEnabled
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
                        onNextClick = { playerViewModel?.playerUiController?.skipToNextMedia() }
                    )
                },
                buttons = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧按钮：输出选择 (药丸型)
                        Box(contentAlignment = Alignment.TopEnd) {
                            Box(
                                modifier = Modifier
                                    .width(54.dp)
                                    .height(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable { onOutputClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Headphones,
                                    contentDescription = "输出",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            
                            // 波浪形音量小角标
                            Box(
                                modifier = Modifier
                                    .offset(x = 4.dp, y = (-2).dp)
                                    .size(18.dp)
                                    .clip(SquigglyBadgeShape()) 
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = Color.Black
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        // 右侧按钮：播放模式切换 (药丸型)
                        Box(
                            modifier = Modifier
                                .width(54.dp)
                                .height(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable {
                                    controller?.let {
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
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val modeIcon = when {
                                shuffleEnabled -> Icons.Default.Shuffle
                                repeatMode == Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            }
                            Icon(
                                modeIcon, 
                                contentDescription = "播放模式", 
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
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
                style = MaterialTheme.typography.titleMedium, 
                textAlign = TextAlign.Center, 
                marqueeDpPerSecond = 15.dp, 
                edgeGradientWidth = 30.dp
            )
        }
        AnimatedContent(targetState = artist, transitionSpec = { (slideInHorizontally(tween(90)) { (it * 0.125f).roundToInt() } + fadeIn(tween(90))) togetherWith (slideOutHorizontally(tween(30)) { (-it * 0.125f).roundToInt() } + fadeOut(tween(30))) }, label = "") { currentArtist ->
            Text(text = currentArtist.orEmpty(), modifier = Modifier.fillMaxWidth().padding(top = 1.dp), color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun LyricsScreenContent(controller: MediaController?, detectLyrics: Boolean) {
    val appContext = LocalContext.current
    val repository = remember { MediaRepository(appContext) }
    var lyricLines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
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
    
    val columnState = rememberScalingLazyListState()
    LaunchedEffect(activeIndex) { if (activeIndex >= 0) columnState.animateScrollToItem(activeIndex + 1) }

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(modifier = Modifier.fillMaxSize().fadingEdge(), state = columnState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 50.dp)) {
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

@Composable
fun OutputDevicePickerScreen(controller: MediaController?, onConnectClick: () -> Unit, onThisWatchClick: () -> Unit) {
    val columnState = rememberScalingLazyListState()
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

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 30.dp, top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { ListHeader { Text("播放控件") } }
            
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
                        Icon(Icons.Default.Remove, null)
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
                        Icon(Icons.Default.Add, null)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                FilledTonalButton(
                    onClick = onConnectClick,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    label = { Text("连接设备") },
                    icon = { Icon(imageVector = Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                FilledTonalButton(
                    onClick = onThisWatchClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("这只手表") },
                    icon = { Icon(imageVector = Icons.Default.Watch, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary) }
                )
            }
        }
    }
}
