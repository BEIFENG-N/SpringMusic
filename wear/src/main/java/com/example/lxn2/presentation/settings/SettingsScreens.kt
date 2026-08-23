package com.example.lxn2.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.lxn2.presentation.components.LXNSwitchButton
import com.example.lxn2.presentation.components.fadingEdge
import com.example.lxn2.presentation.theme.ThemeColors
import kotlinx.coroutines.flow.StateFlow

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
    ScreenScaffold(
        scrollState = columnState,
        timeText = { } // 明确尝试覆盖时间
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 30.dp, top = 26.dp) // 大幅增加顶部间距，避免与时间重叠
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
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
    onSourceManagementClick: () -> Unit, 
    onLyricsSettingsClick: () -> Unit,
    onThemeSettingsClick: () -> Unit
) {
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text("设置") } }
            item { 
                FilledTonalButton(
                    onClick = onScanLocalMusicClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("本地音乐扫描") },
                    icon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            item { 
                FilledTonalButton(
                    onClick = onSourceManagementClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("自定义源管理") },
                    icon = { Icon(Icons.Default.Extension, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            item { 
                FilledTonalButton(
                    onClick = { onLyricsSettingsClick() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("歌词设置") },
                    icon = { Icon(Icons.Default.Lyrics, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            item { 
                FilledTonalButton(
                    onClick = onThemeSettingsClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("应用主题") },
                    icon = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
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
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("应用主题") } }
            item {
                LXNSwitchButton(
                    checked = blurEnabled,
                    onCheckedChange = onToggleBlur,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("背景模糊") }
                )
            }
            item {
                FilledTonalButton(
                    onClick = onThemeColorClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("主题色") }
                )
            }
        }
    }
}

@Composable
fun ThemeColorPickerScreen(onColorSelected: (Color) -> Unit) {
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("选择颜色") } }
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
fun LyricsSettingsScreen(detectEnabled: Boolean, onToggleDetect: (Boolean) -> Unit) {
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text("歌词设置") } }
            item { 
                LXNSwitchButton(
                    checked = detectEnabled, 
                    onCheckedChange = onToggleDetect, 
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("检测歌曲内嵌歌词") }
                ) 
            }
        }
    }
}
