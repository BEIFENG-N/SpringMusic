# 音乐软件开发计划 (Online + Local Music Player)

本项目将基于 **Jetpack Media3** 和 **Google Horologist** 构建一个支持在线和本地播放的音乐播放器，涵盖手机 (Mobile) 和手表 (Wear OS) 端。

## Proposed Changes

### 1. 基础架构与依赖 (Core Architecture & Dependencies)
使用 **Media3** 作为播放引擎，确保手机端和手表端的播放逻辑一致。

#### [MODIFY] [libs.versions.toml](file:///C:/Users/kalil/AndroidStudioProjects/LXN/gradle/libs.versions.toml)
* 添加 Media3 依赖 (ExoPlayer, Session)。
* 添加 Horologist 依赖 (针对 Wear OS 端)。

#### [MODIFY] [mobile/build.gradle](file:///C:/Users/kalil/AndroidStudioProjects/LXN/mobile/build.gradle)
* 引入 Media3 依赖。

#### [MODIFY] [wear/build.gradle](file:///C:/Users/kalil/AndroidStudioProjects/LXN/wear/build.gradle)
* 引入 Media3 和 Horologist 依赖。

---

### 2. 播放服务 (Playback Service)
实现一个跨进程的 `MediaSessionService`，负责后台播放逻辑。

#### [NEW] [PlaybackService.kt](file:///C:/Users/kalil/AndroidStudioProjects/LXN/mobile/src/main/java/com/example/lxn2/service/PlaybackService.kt)
* 初始化 `ExoPlayer`。
* 管理 `MediaSession`。
* 处理播放、暂停、切歌等指令。

---

### 3. 数据管理 (Data Management)
管理本地音乐 (ContentResolver) 和在线音乐 (URL 列表)。

#### [NEW] [MediaRepository.kt](file:///C:/Users/kalil/AndroidStudioProjects/LXN/mobile/src/main/java/com/example/lxn2/data/MediaRepository.kt)
* 获取本地音频文件。
* 获取在线音频资源。

---

### 4. 手机端 UI (Mobile UI)
使用 Compose 实现基础播放界面。

#### [MODIFY] [MainActivity.kt](file:///C:/Users/kalil/AndroidStudioProjects/LXN/mobile/src/main/java/com/example/lxn2/MainActivity.kt)
* 连接 `PlaybackService`。
* 显示播放列表和控制按钮。

---

### 5. 手表端 UI (Wear OS UI)
利用 **Horologist** 库提供的组件构建适配手表的播放界面。

#### [MODIFY] [MainActivity.kt](file:///C:/Users/kalil/AndroidStudioProjects/LXN/wear/src/main/java/com/example/lxn2/presentation/MainActivity.kt)
* 使用 Horologist 的 `MediaPlayerScreen` 或自定义控制组件。

## Verification Plan

### Automated Tests
* 验证 `PlaybackService` 的生命周期管理。
* 验证本地音乐扫描逻辑。

### Manual Verification
* 在手机端播放本地/在线音乐。
* 在手表端通过蓝牙耳机播放音乐。
* 验证后台播放通知栏控制。
