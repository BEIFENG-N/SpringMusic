# SpringMusic for Wear OS 🎵

SpringMusic 是一款专为 **Android Wear OS 智能手表** 打造的高颜值、现代化的独立音乐播放器。基于 Jetpack Compose for Wear OS Material 3 架构与 Media3 ExoPlayer 框架构建，支持本地音乐播放、自定义 QuickJS 音源解析、内嵌歌词显示与高度可定制的界面主题。

---

## ✨ 核心特性

- ⌚ **原生 Wear OS 体验**：基于 Wear OS Material 3 规范与 Horologist 库开发，完美适配圆屏与方屏智能手表。
- 🎵 **独立流媒体与本地播放**：
  - 支持本地音频文件（MP3, FLAC, WAV）扫描与音轨分类（歌单/艺人/专辑）。
  - 支持基于 QuickJS 引擎的自定义音源脚本拓展，可实现线上音乐搜索与在线流媒体直连播放。
- 📖 **内嵌与在线歌词显示**：支持解析音频文件内嵌 LRC 歌词与翻译歌词，高亮滚动与二分查找算法平滑同步。
- 🎨 **主题与个性化定制**：
  - 15+ 款精致主题色彩一键切换。
  - 支持 Glassmorphism（毛玻璃）与 Material Design 界面风格。
  - 支持界面整体缩放（80% ~ 120%），适配不同表盘尺寸。
  - 硬件加速高斯模糊背景与平滑过度蒙层。
- ⚡ **性能与低功耗优化**：
  - 专门针对手表端 RAM 与电池优化的 ExoPlayer 缓冲策略。
  - 原生 `MediaMetadataRetriever` C++ 底层音轨元数据与封面提取。
  - 搜索 500ms 防抖节流、分页加载与 LRU 内存缓存。
  - 磁盘缓存统计与一键清理功能。

---

## 🛠 技术栈 (Tech Stack)

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose for Wear OS Material 3 (`androidx.wear.compose.material3`)
- **音频引擎**：AndroidX Media3 ExoPlayer (`androidx.media3`) & MediaSession
- **手表交互**：Horologist (`com.google.android.horologist`)
- **脚本引擎**：CashApp QuickJS Android (`app.cash.quickjs`)
- **网络与图片**：OkHttp3 & Coil (`coil.compose`)
- **依赖管理**：Gradle Version Catalog (`libs.versions.toml`)

---

## 🚀 编译与构建 (Build & Run)

### 环境要求
- Android Studio Ladybug / Jellyfish (或更新版本)
- JDK 17
- Android SDK 35 / Wear OS 3.0+ (API Level 30+)

### 构建步骤
1. 克隆本仓库：
   ```bash
   git clone https://github.com/BEIFENG-N/SpringMusic.git
   cd SpringMusic
   ```
2. 在 Android Studio 中打开项目，并同步 Gradle 依赖。
3. 连接 Wear OS 手表设备或启动 Wear OS 模拟器。
4. 执行编译构建：
   ```bash
   ./gradlew :wear:assembleDebug
   ```

---

## 📄 开源协议 (License)

本项目采用 [MIT License](LICENSE) 协议开源。
