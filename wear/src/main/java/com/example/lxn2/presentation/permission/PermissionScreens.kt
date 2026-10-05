package com.example.lxn2.presentation.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.lxn2.permission.PermissionManager

/**
 * 首次启动的权限说明屏。
 *
 * 仅展示说明与“去授权”按钮，用户显式点击才通过系统设置授权，
 * 返回后若已授权则回调 [onGranted]，否则停留在本屏并提示。
 * 绝不自动跳转系统设置。
 */
@Composable
fun PermissionScreen(onGranted: () -> Unit, onSkipped: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(PermissionManager.canAccessMusicStorage())
    }
    var shouldFinish by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = PermissionManager.canAccessMusicStorage()
        if (hasPermission) shouldFinish = true
    }

    if (shouldFinish) {
        onGranted()
        return
    }

    val columnState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 24.dp)
        ) {
            item { ListHeader { Text("欢迎使用 SpringMusic") } }
            item {
                Text(
                    "为了播放 /storage/music 中的本地音乐，" +
                        "需要授予“所有文件访问权限”。",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
            if (!hasPermission) {
                item {
                    FilledTonalButton(
                        onClick = {
                            launcher.launch(PermissionManager.buildAllFilesIntent(context))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("去授权") },
                        icon = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                }
                item {
                    Text(
                        "授权后即可加载本地音乐。",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
                item {
                    FilledTonalButton(
                        onClick = onSkipped,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("暂不授权") }
                    )
                }
            } else {
                item {
                    Text(
                        "权限已就绪，点击继续。",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
                item {
                    FilledTonalButton(
                        onClick = { onGranted() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("继续") },
                        icon = { Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
        }
    }
}
