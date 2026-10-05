package com.example.lxn2.presentation.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.lxn2.R
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
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.LevelIndicator
import androidx.wear.compose.material3.LevelIndicatorDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import com.example.lxn2.data.LyricLine
import com.example.lxn2.presentation.components.SquigglyBadgeShape
import com.example.lxn2.presentation.components.fadingEdge
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumeUiState
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
import kotlinx.coroutines.flow.collectLatest

@Composable
fun VolumeLevelIndicator(
    volumeUiState: () -> VolumeUiState,
    displayIndicatorEvents: kotlinx.coroutines.flow.Flow<Unit>?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sweepAngle: Float = 54f
) {
    val visible by produceState(
        initialValue = displayIndicatorEvents == null,
        key1 = displayIndicatorEvents
    ) {
        displayIndicatorEvents?.collectLatest {
            value = true
            delay(2000)
            value = false
        }
    }
    val uiState = volumeUiState()
    val fraction = remember(uiState.current, uiState.min, uiState.max) {
        if (uiState.max > uiState.min) {
            ((uiState.current - uiState.min).toFloat() / (uiState.max - uiState.min).toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        LevelIndicator(
            value = { fraction },
            modifier = Modifier.fillMaxSize(),
            enabled = enabled,
            sweepAngle = sweepAngle,
            colors = LevelIndicatorDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    }
}

@Composable
fun CustomMediaControlButtons(
    playing: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(58.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 上一首按钮
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(42.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.btn_skip_previous), modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 2. 播放/暂停按钮容器
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(58.dp)) {
            Box(
                modifier = Modifier
                    .size(50.dp) 
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer) 
                    .clickable { onPlayPauseClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 
                    contentDescription = stringResource(R.string.btn_play_pause), 
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer 
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 3. 下一首按钮
        IconButton(
            onClick = onNextClick,
            modifier = Modifier.size(42.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.btn_skip_next), modifier = Modifier.size(22.dp))
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
    val displayTitle = readyMedia?.title ?: fallbackTitle ?: stringResource(R.string.no_song_playing)
    val displayArtist = readyMedia?.subtitle ?: fallbackArtist ?: stringResource(R.string.please_scan_local_music)

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
                                    contentDescription = stringResource(R.string.btn_output_device),
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
                                contentDescription = stringResource(R.string.btn_play_mode), 
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                },
                modifier = Modifier
                    .requestFocusOnHierarchyActive()
                    .onRotaryScrollEvent { true } // 兜底拦截：确保即便音量到头，事件也不会冒泡给 Pager
                    .rotaryScrollable(
                        behavior = volumeRotaryBehavior(
                            volumeUiStateProvider = { volumeUiState }, 
                            onRotaryVolumeInput = { newVolume -> volumeViewModel.setVolume(newVolume) }
                        ),
                        focusRequester = remember { FocusRequester() }
                    )
            )
            VolumeLevelIndicator(
                volumeUiState = { volumeUiState },
                displayIndicatorEvents = volumeViewModel.displayIndicatorEvents
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

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun LyricsScreenContent(
    controller: MediaController?, 
    detectLyrics: Boolean,
    persistence: com.example.lxn2.data.PlaybackPersistence
) {
    val appContext = LocalContext.current
    val repository = remember { (appContext.applicationContext as com.example.lxn2.LXNApplication).container.mediaRepository }
    var lyricLines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    val showTranslation = persistence.isShowLyricTranslation()
    
    var currentMediaItem by remember { mutableStateOf(controller?.currentMediaItem) }
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    currentMediaItem = player.currentMediaItem
                    currentPosition = 0L
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                currentPosition = newPosition.positionMs
            }
        }
        controller?.addListener(listener)
        currentMediaItem = controller?.currentMediaItem
        currentPosition = controller?.currentPosition ?: 0L
        onDispose { controller?.removeListener(listener) }
    }

    LaunchedEffect(currentMediaItem, detectLyrics, showTranslation) {
        lyricLines = if (currentMediaItem != null && detectLyrics) {
            repository.getParsedLyrics(currentMediaItem!!, showTranslation = showTranslation)
        } else {
            emptyList()
        }
    }

    // 二分查找当前时间对应的歌词高亮行（lyricLines 按 timeMs 升序）
    val activeIndex = remember(lyricLines, currentPosition) {
        var low = 0
        var high = lyricLines.lastIndex
        var result = -1
        while (low <= high) {
            val mid = (low + high).ushr(1)
            if (lyricLines[mid].timeMs <= currentPosition) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        result
    }
    
    val columnState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(activeIndex) { if (activeIndex >= 0) columnState.animateScrollToItem(activeIndex + 1) }

    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .fadingEdge()
                .requestFocusOnHierarchyActive()
                .rotaryScrollable(
                    behavior = RotaryScrollableDefaults.behavior(columnState),
                    focusRequester = focusRequester
                ),
            state = columnState, 
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 50.dp)
        ) {
            item { ListHeader { Text(stringResource(R.string.lyrics_title)) } }
            if (lyricLines.isEmpty()) {
                item { Text(text = if (detectLyrics) stringResource(R.string.no_lyrics) else stringResource(R.string.lyrics_disabled), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            } else {
                items(lyricLines) { line: LyricLine ->
                    val isActive = line == lyricLines.getOrNull(activeIndex)
                    val alpha by animateFloatAsState(if (isActive) 1f else 0.5f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(alpha)
                    ) {
                        Text(
                            text = line.text, 
                            textAlign = TextAlign.Center, 
                            fontSize = if (isActive) 16.sp else 14.sp, 
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, 
                            color = if (isActive) Color.White else Color.LightGray, 
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (showTranslation && !line.translation.isNullOrBlank()) {
                            Text(
                                text = line.translation,
                                textAlign = TextAlign.Center,
                                fontSize = if (isActive) 13.sp else 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.fillMaxWidth().padding(top = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun OutputDevicePickerScreen(
    controller: MediaController?,
    volumeViewModel: VolumeViewModel,
    onConnectClick: () -> Unit,
    onThisWatchClick: () -> Unit
) {
    val columnState = rememberScalingLazyListState()
    val volumeUiState by volumeViewModel.volumeUiState.collectAsStateWithLifecycle()

    val volumeFraction = if (volumeUiState.max > volumeUiState.min) {
        ((volumeUiState.current - volumeUiState.min).toFloat() / (volumeUiState.max - volumeUiState.min).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val focusRequester = remember { FocusRequester() }

    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .fadingEdge()
                    .requestFocusOnHierarchyActive()
                    .onRotaryScrollEvent { true }
                    .rotaryScrollable(
                        behavior = RotaryScrollableDefaults.behavior(columnState),
                        focusRequester = focusRequester
                    ),
                state = columnState,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { ListHeader { Text(stringResource(R.string.playback_controls)) } }
                
                // Volume Control Pill
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { volumeViewModel.decreaseVolumeWithHaptics() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.decrease_volume))
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
                                    .fillMaxWidth(volumeFraction)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

                        IconButton(
                            onClick = { volumeViewModel.increaseVolumeWithHaptics() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.increase_volume))
                        }
                    }
                }

                item {
                    FilledTonalButton(
                        onClick = onConnectClick,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.connect_device)) },
                        icon = { Icon(imageVector = Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary) }
                    )
                }

                item {
                    FilledTonalButton(
                        onClick = onThisWatchClick,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.this_watch)) },
                        icon = { Icon(imageVector = Icons.Default.Watch, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary) }
                    )
                }
            }
        VolumeLevelIndicator(
            volumeUiState = { volumeUiState },
            displayIndicatorEvents = volumeViewModel.displayIndicatorEvents
        )
    }
}
}

@Composable
fun TempPlayListScreen(
    onRemove: (Int) -> Unit,
    onClear: () -> Unit
) {
    val tempItems by com.example.lxn2.data.core.PlayerCore.tempPlayList.collectAsStateWithLifecycle()
    val columnState = rememberScalingLazyListState()

    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("${stringResource(R.string.temp_playlist_title)} (${tempItems.size})") } }

            if (tempItems.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.temp_playlist_empty),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            } else {
                item {
                    FilledTonalButton(
                        onClick = onClear,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        label = { Text(stringResource(R.string.clear_temp_playlist)) }
                    )
                }
                items(tempItems.indices.toList()) { index ->
                    val item = tempItems[index]
                    FilledTonalButton(
                        onClick = { onRemove(index) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        label = {
                            Text(
                                text = "${index + 1}. ${item.mediaMetadata.title ?: "未知曲目"}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = item.mediaMetadata.artist?.toString() ?: "未知歌手",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    }
}
