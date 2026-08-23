package com.example.lxn2.presentation.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.lxn2.presentation.components.LXNSwitchButton
import com.example.lxn2.presentation.components.fadingEdge
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import kotlin.math.roundToInt

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
    
    val navEntry = navController.currentBackStackEntry
    val h = navEntry?.savedStateHandle?.get<Int>("h") ?: initialHours
    val m = navEntry?.savedStateHandle?.get<Int>("m") ?: initialMinutes
    val s = navEntry?.savedStateHandle?.get<Int>("s") ?: initialSeconds

    val columnState = rememberScalingLazyListState()

    ScreenScaffold(
        scrollState = columnState,
        edgeButton = {
            EdgeButton(
                onClick = { 
                    if (timerEnabled) {
                        onConfirm(h * 3600 + m * 60 + s, finishSong)
                    } else {
                        onCancel()
                    }
                }
            ) {
                Icon(Icons.Default.Check, null)
            }
        }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 30.dp, top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                LXNSwitchButton(
                    checked = timerEnabled,
                    onCheckedChange = { timerEnabled = it },
                    label = { Text("启用定时", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LXNSwitchButton(
                    checked = finishSong,
                    onCheckedChange = { finishSong = it },
                    enabled = timerEnabled,
                    label = { Text("播放完整歌曲", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (timerEnabled) {
                item {
                    FilledTonalButton(
                        onClick = onTimeClick,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "设定时长", 
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
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
fun ScanningOverlay(scanned: Int, total: Int) {
    val progress = if (total > 0) (scanned.toFloat() / total * 100).roundToInt() else 0
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$progress%", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "$scanned/$total", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        }
    }
}
