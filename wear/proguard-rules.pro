# QuickJS Native Engine
-keep class app.cash.quickjs.** { *; }
-dontwarn app.cash.quickjs.**

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Horologist Wear OS Components
-keep class com.google.android.horologist.** { *; }
-dontwarn com.google.android.horologist.**

# Jaudiotagger Audio Metadata Parser
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
