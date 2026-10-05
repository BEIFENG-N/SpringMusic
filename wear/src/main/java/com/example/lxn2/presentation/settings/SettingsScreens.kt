package com.example.lxn2.presentation.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.session.MediaController
import com.example.lxn2.data.PlaybackPersistence
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.lxn2.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.example.lxn2.presentation.components.LXNSwitchButton
import com.example.lxn2.presentation.components.fadingEdge
import com.example.lxn2.presentation.theme.ThemeColors
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MoreScreen(
    onLibraryClick: () -> Unit,
    onPlayListClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTimerClick: () -> Unit,
    remainingTimeFlow: StateFlow<Long?>,
    isFocused: Boolean = true
) {
    val remainingTime by remainingTimeFlow.collectAsStateWithLifecycle()
    val columnState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
        }
    }

    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) },
        timeText = { }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .fadingEdge()
                .requestFocusOnHierarchyActive()
                .onRotaryScrollEvent { true } // 兜底拦截：核心修复，防止列表滚到顶部后冒泡，且不干扰焦点
                .rotaryScrollable(
                    behavior = RotaryScrollableDefaults.behavior(columnState),
                    focusRequester = focusRequester
                ),
            state = columnState,
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 30.dp, top = 26.dp) 
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
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onSearchClick,
                            modifier = Modifier.weight(1f).height(42.dp),
                            label = {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        )
                        FilledTonalButton(
                            onClick = onLibraryClick, // 点击进入库
                            modifier = Modifier.weight(1f).height(42.dp),
                            label = {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.LibraryMusic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        )
                    }
                }
            }

            item { 
                FilledTonalButton(
                    onClick = onPlayListClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("播放列表") },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            item { 
                FilledTonalButton(
                    onClick = onTimerClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
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
                    },
                    icon = { Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            item { 
                FilledTonalButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("更多设置") },
                    icon = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onScanLocalMusicClick: () -> Unit,
    onPlayerSettingsClick: () -> Unit,
    onSourceManagementClick: () -> Unit, 
    onLyricsSettingsClick: () -> Unit,
    onAppInterfaceClick: () -> Unit,
    onCacheManagementClick: () -> Unit = {}
) {
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text(stringResource(R.string.settings_title)) } }
            item { 
                FilledTonalButton(
                    onClick = onScanLocalMusicClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_scan_music)) },
                    icon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            item { 
                FilledTonalButton(
                    onClick = onPlayerSettingsClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_player)) },
                    icon = { Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            item { 
                FilledTonalButton(
                    onClick = onSourceManagementClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_source)) },
                    icon = { Icon(Icons.Default.Extension, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            item { 
                FilledTonalButton(
                    onClick = onLyricsSettingsClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_lyrics)) },
                    icon = { Icon(Icons.Default.Lyrics, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            item { 
                FilledTonalButton(
                    onClick = onAppInterfaceClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_app_interface)) },
                    icon = { Icon(Icons.Default.DisplaySettings, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            item { 
                FilledTonalButton(
                    onClick = onCacheManagementClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_cache_management)) },
                    icon = { Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
        }
    }
}

@Composable
fun CacheManagementScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val columnState = rememberScalingLazyListState()
    var cacheBytes by remember { mutableLongStateOf(com.example.lxn2.data.MediaCacheManager.getCacheSizeBytes(context)) }

    val cacheMb = cacheBytes / (1024f * 1024f)
    val toastMsg = stringResource(R.string.cache_cleared_toast)

    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { ListHeader { Text(stringResource(R.string.settings_cache_management)) } }
            item {
                Text(
                    text = stringResource(R.string.cache_used_size, cacheMb),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                FilledTonalButton(
                    onClick = {
                        com.example.lxn2.data.MediaCacheManager.clearCache(context)
                        cacheBytes = com.example.lxn2.data.MediaCacheManager.getCacheSizeBytes(context)
                        android.widget.Toast.makeText(
                            context,
                            toastMsg,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.btn_clear_cache)) },
                    icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}

@Composable
fun AppInterfaceSettingsScreen(
    onThemeSettingsClick: () -> Unit,
    onScaleSettingsClick: () -> Unit = {}
) {
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text(stringResource(R.string.settings_app_interface)) } }
            item { 
                FilledTonalButton(
                    onClick = onThemeSettingsClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_theme)) },
                    icon = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            item { 
                FilledTonalButton(
                    onClick = onScaleSettingsClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_scale)) },
                    icon = { Icon(Icons.Default.FitScreen, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
        }
    }
}

@Composable
fun ScaleSettingsScreen(
    currentScale: Float,
    onScaleChange: (Float) -> Unit
) {
    val columnState = rememberScalingLazyListState()
    val percentage = (currentScale * 100).roundToInt()
    val fraction = ((currentScale - 0.80f) / (1.20f - 0.80f)).coerceIn(0f, 1f)

    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { ListHeader { Text("界面缩放") } }

            item {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Scale Control Pill (- / Progress bar / +)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newScale = ((currentScale - 0.05f) * 100).roundToInt() / 100f
                            onScaleChange(newScale.coerceIn(0.80f, 1.20f))
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = currentScale > 0.801f
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "减小缩放")
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

                    IconButton(
                        onClick = {
                            val newScale = ((currentScale + 0.05f) * 100).roundToInt() / 100f
                            onScaleChange(newScale.coerceIn(0.80f, 1.20f))
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = currentScale < 1.199f
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "增大缩放")
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerSettingsScreen(
    persistence: PlaybackPersistence,
    controller: MediaController?
) {
    val columnState = rememberScalingLazyListState()
    var currentRate by remember { mutableFloatStateOf(persistence.getPlaybackRate()) }
    var autoClean by remember { mutableStateOf(persistence.isAutoCleanPlayedList()) }
    var addMusicTop by remember { mutableStateOf(persistence.isAddMusicLocationTop()) }

    val rates = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text(stringResource(R.string.settings_player)) } }

            item {
                LXNSwitchButton(
                    checked = autoClean,
                    onCheckedChange = {
                        autoClean = it
                        persistence.saveAutoCleanPlayedList(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_auto_clean_played)) }
                )
            }

            item {
                LXNSwitchButton(
                    checked = addMusicTop,
                    onCheckedChange = {
                        addMusicTop = it
                        persistence.saveAddMusicLocationTop(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_add_music_location)) }
                )
            }

            item { ListHeader { Text("播放倍速 (${currentRate}x)") } }

            items(rates) { rate ->
                val isSelected = currentRate == rate
                FilledTonalButton(
                    onClick = {
                        currentRate = rate
                        persistence.savePlaybackRate(rate)
                        controller?.setPlaybackSpeed(rate)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("${rate}x") },
                    icon = if (isSelected) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun ThemeSettingsScreen(
    onDisplayEffectClick: () -> Unit,
    onThemeColorClick: () -> Unit
) {
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text(stringResource(R.string.settings_theme)) } }
            item {
                FilledTonalButton(
                    onClick = onDisplayEffectClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_display_effect)) },
                    icon = { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) }
                )
            }
            item {
                FilledTonalButton(
                    onClick = onThemeColorClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_theme_color)) },
                    icon = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) }
                )
            }
        }
    }
}

@Composable
fun DisplayEffectSettingsScreen(
    blurEnabled: Boolean, 
    onToggleBlur: (Boolean) -> Unit,
    displayEffect: String,
    onDisplayEffectChange: (String) -> Unit
) {
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text(stringResource(R.string.settings_display_effect)) } }
            item {
                LXNSwitchButton(
                    checked = blurEnabled,
                    onCheckedChange = onToggleBlur,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_background_blur)) }
                )
            }
            item { ListHeader { Text("设计风格") } }
            item {
                LXNSwitchButton(
                    checked = displayEffect == "GLASSMORPHISM",
                    onCheckedChange = { checked ->
                        if (checked) {
                            onDisplayEffectChange("GLASSMORPHISM")
                        } else {
                            onDisplayEffectChange("MATERIAL_DESIGN")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Glassmorphism") }
                )
            }
            item {
                LXNSwitchButton(
                    checked = displayEffect == "MATERIAL_DESIGN",
                    onCheckedChange = { checked ->
                        if (checked) {
                            onDisplayEffectChange("MATERIAL_DESIGN")
                        } else {
                            onDisplayEffectChange("GLASSMORPHISM")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Material Design") }
                )
            }
        }
    }
}

@Composable
fun ThemeColorPickerScreen(onColorSelected: (Color) -> Unit) {
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
            item { ListHeader { Text(stringResource(R.string.settings_pick_color)) } }
            items(ThemeColors) { themeColor ->
                FilledTonalButton(
                    onClick = { onColorSelected(themeColor.color) },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
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
                )
            }
        }
    }
}

@Composable
fun LyricsSettingsScreen(
    persistence: PlaybackPersistence,
    detectEnabled: Boolean, 
    onToggleDetect: (Boolean) -> Unit
) {
    val columnState = rememberScalingLazyListState()
    var showTranslation by remember { mutableStateOf(persistence.isShowLyricTranslation()) }
    var showRoma by remember { mutableStateOf(persistence.isShowLyricRoma()) }

    ScreenScaffold(
        scrollState = columnState,
        scrollIndicator = { ScrollIndicator(columnState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text(stringResource(R.string.settings_lyrics)) } }
            item { 
                LXNSwitchButton(
                    checked = detectEnabled, 
                    onCheckedChange = onToggleDetect, 
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_detect_embedded_lyrics)) }
                ) 
            }
            item { 
                LXNSwitchButton(
                    checked = showTranslation, 
                    onCheckedChange = {
                        showTranslation = it
                        persistence.saveShowLyricTranslation(it)
                    }, 
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("显示翻译歌词") }
                ) 
            }
            item { 
                LXNSwitchButton(
                    checked = showRoma, 
                    onCheckedChange = {
                        showRoma = it
                        persistence.saveShowLyricRoma(it)
                    }, 
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("显示罗马音歌词") }
                ) 
            }
        }
    }
}
