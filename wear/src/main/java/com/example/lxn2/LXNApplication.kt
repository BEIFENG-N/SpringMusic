package com.example.lxn2

import android.app.Application
import com.example.lxn2.di.AppContainer

/**
 * 应用级入口，持有应用级依赖容器 [AppContainer]。
 */
class LXNApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
