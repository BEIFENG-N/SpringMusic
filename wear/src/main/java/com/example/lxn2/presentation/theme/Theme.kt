package com.example.lxn2.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography

data class ThemeColor(val name: String, val color: Color)

val ThemeColors = listOf(
    ThemeColor("天空蓝", Color(0xFFCDE7FF)),
    ThemeColor("海蓝色", Color(0xFFAECBFF)),
    ThemeColor("宝石蓝", Color(0xFF82B1FF)),
    ThemeColor("水晶紫", Color(0xFFD0BCFF)),
    ThemeColor("小麦色", Color(0xFFFDE1B8)),
    ThemeColor("蒲公英黄", Color(0xFFFFF690)),
    ThemeColor("柠檬酒黄", Color(0xFFFDFFC2)),
    ThemeColor("香草绿", Color(0xFFB7E4C7)),
    ThemeColor("番石榴粉", Color(0xFFE3AFAC)),
    ThemeColor("桃红色", Color(0xFFFFBFA3)),
    ThemeColor("香槟色", Color(0xFFFFD9BD)),
    ThemeColor("奶茶色", Color(0xFFC1A38E)),
    ThemeColor("石墨黑", Color(0xFF969696)),
    ThemeColor("云白色", Color(0xFFE1E1E1)),
    ThemeColor("杏仁白", Color(0xFFF5E6D3))
)

fun createColorScheme(primary: Color): ColorScheme {
    return ColorScheme(
        primary = primary,
        onPrimary = Color(0xFF1E1E1E), // 确保在亮色 track 上有足够的对比度
        primaryContainer = primary.copy(alpha = 0.2f),
        onPrimaryContainer = Color.White,
        secondary = primary.copy(alpha = 0.7f),
        onSecondary = Color(0xFF1E1E1E),
        secondaryContainer = primary.copy(alpha = 0.15f),
        onSecondaryContainer = Color.White,
        tertiary = primary.copy(alpha = 0.5f),
        onTertiary = Color(0xFF1E1E1E),
        tertiaryContainer = primary.copy(alpha = 0.1f),
        onTertiaryContainer = Color.White,
        background = Color(0xFF000000),
        onBackground = Color.White,
        surfaceContainer = Color(0xFF1E1E1E),
        onSurface = Color.White,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005)
    )
}

val wearTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp
    )
)

@Composable
fun LXNTheme(
    primaryColor: Color = Color(0xFFB7E4C7),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = createColorScheme(primaryColor),
        typography = wearTypography,
        content = content
    )
}