package com.example.lxn2.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/**
 * 应用权限统一中枢。
 *
 * 应用播放的是用户自定义目录（如 /storage/music/...）中的本地音乐，
 * 必须依赖 [MANAGE_EXTERNAL_STORAGE] 特殊权限（“所有文件访问权限”）。
 * 特殊权限无法通过运行时弹窗授予，只能引导用户进入系统设置手动开启，
 * 因此本类集中提供“权限状态判断”与“跳转系统设置授权”两件事，
 * 并保证**绝不自动触发跳转**——一切授权跳转都必须由用户显式点击发起。
 */
object PermissionManager {

    /** 音乐目录是否可访问（是否已授予“所有文件访问权限”）。 */
    fun canAccessMusicStorage(): Boolean = Environment.isExternalStorageManager()

    /**
     * 构造跳转到本应用“所有文件访问权限”设置页的 Intent。
     * 若系统无法定位到具体应用页，会回退到所有应用列表页。
     */
    fun buildAllFilesIntent(context: Context): Intent {
        val packageName = context.packageName
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return if (intent.resolveActivity(context.packageManager) != null) {
            intent
        } else {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        }
    }

    /** 权限拒绝后的系统提示文案。 */
    fun permissionHint(): String =
        "本应用需要“所有文件访问权限”才能读取 /storage/music 中的本地音乐，" +
            "请在上方点击“去授权”后在系统设置中开启。"
}

/** 当环境支持 `READ_MEDIA_AUDIO` 时返回其权限名，否则返回空（无需声明）。 */
@Suppress("DEPRECATION")
val mediaAudioPermission: String?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        null
    }
