package com.example.lxn2.presentation.source

import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.lxn2.data.PlaybackPersistence
import com.example.lxn2.permission.PermissionManager
import com.example.lxn2.presentation.components.fadingEdge
import java.io.File

@Composable
fun SourceManagementScreen(persistence: PlaybackPersistence, onAddSourceClick: () -> Unit) {
    val columnState = rememberScalingLazyListState()
    val activeSource = persistence.getActiveSourcePath()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text("源管理") } }
            item {
                FilledTonalButton(
                    onClick = onAddSourceClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Column {
                            Text("当前源: ${if (activeSource != null) File(activeSource).name else "未设置"}")
                            Text(activeSource ?: "点击选择 .js 文件", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    icon = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }
                )
            }
            if (activeSource != null) {
                item {
                    FilledTonalButton(
                        onClick = { persistence.saveActiveSourcePath(null) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("清除当前源") },
                        icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
        }
    }
}

@Composable
fun JsFilePickerScreen(onFileSelected: (File) -> Unit) {
    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }

    // 不再自动弹权限，仅以当前状态决定是否显示目录与引导入口。
    var hasPermission by remember {
        mutableStateOf(PermissionManager.canAccessMusicStorage())
    }

    val appContext = LocalContext.current
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = PermissionManager.canAccessMusicStorage()
    }

    val entries = remember(currentDir, hasPermission) { 
        val list = currentDir.listFiles() ?: emptyArray()
        list.filter { file -> 
            file.isDirectory || file.name.endsWith(".js", ignoreCase = true) 
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    val columnState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("选择 JS 脚本") } }
            
            if (!hasPermission) {
                item {
                    Text(PermissionManager.permissionHint(), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
                item {
                    FilledTonalButton(
                        onClick = { settingsLauncher.launch(PermissionManager.buildAllFilesIntent(appContext)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("去授权") }
                    )
                }
            }

            if (currentDir != Environment.getExternalStorageDirectory()) { 
                item { 
                    FilledTonalButton(
                        onClick = { currentDir = currentDir.parentFile ?: currentDir }, 
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("返回") },
                        icon = { Icon(Icons.Default.ArrowUpward, null, tint = MaterialTheme.colorScheme.primary) }
                    ) 
                } 
            }
            
            items(entries) { file: File -> 
                FilledTonalButton(
                    onClick = { if (file.isDirectory) currentDir = file else onFileSelected(file) }, 
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
        }
    }
}

@Composable
fun LocalMusicScanMenuScreen(currentPath: String?, onCustomPathClick: () -> Unit, onScanClick: () -> Unit) {
    val columnState = rememberScalingLazyListState()
    val appContext = LocalContext.current

    // 不做自动跳转；仅在未授权时展示一个手动“去授权”入口。
    var hasPermission by remember {
        mutableStateOf(PermissionManager.canAccessMusicStorage())
    }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = PermissionManager.canAccessMusicStorage()
    }

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp)
        ) {
            item { ListHeader { Text("本地音乐扫描") } }
            if (!hasPermission) {
                item {
                    Text(PermissionManager.permissionHint(), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
                item {
                    FilledTonalButton(
                        onClick = { settingsLauncher.launch(PermissionManager.buildAllFilesIntent(appContext)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("去授权") },
                        icon = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
            item { 
                FilledTonalButton(
                    onClick = onCustomPathClick,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Column {
                            Text("自定义扫描目录")
                            Text(currentPath ?: "未选择", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    icon = { Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            item { 
                FilledTonalButton(
                    onClick = { onScanClick() },
                    enabled = currentPath != null && hasPermission, 
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("加载音乐文件") },
                    icon = { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
        }
    }
}

@Composable
fun FolderPickerScreen(onFolderSelected: (File) -> Unit) {
    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    val files = remember(currentDir) { currentDir.listFiles { file -> file.isDirectory }?.toList()?.sortedBy { it.name } ?: emptyList() }
    val columnState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdge(),
            state = columnState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp)
        ) {
            item { ListHeader { Text("选择目录") } }
            item { 
                FilledTonalButton(
                    onClick = { onFolderSelected(currentDir) }, 
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("选择该文件夹") },
                    icon = { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
            if (currentDir != Environment.getExternalStorageDirectory()) { 
                item { 
                    FilledTonalButton(
                        onClick = { currentDir = currentDir.parentFile ?: currentDir }, 
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("返回") },
                        icon = { Icon(Icons.Default.ArrowUpward, null, tint = MaterialTheme.colorScheme.primary) }
                    ) 
                } 
            }
            item { Text(text = currentDir.absolutePath, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
            items(files) { file: File -> 
                FilledTonalButton(
                    onClick = { currentDir = file }, 
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(file.name) },
                    icon = { Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary) }
                ) 
            }
        }
    }
}
