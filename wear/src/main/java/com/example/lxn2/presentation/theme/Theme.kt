package com.example.lxn2.presentation.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.SwitchButtonDefaults
import androidx.wear.compose.material3.SwitchButtonColors
import androidx.wear.compose.material3.dynamicColorScheme

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

/**
 * Wear Material 3 调色板定义。
 * 遵循准则：背景必须为纯黑以适配 OLED 并隐藏物理边框。
 */
fun createColorScheme(primary: Color): ColorScheme {
    return ColorScheme(
        primary = primary,
        onPrimary = Color.Black,
        primaryContainer = primary.copy(alpha = 0.2f),
        onPrimaryContainer = Color.Black,
        secondary = primary.copy(alpha = 0.7f),
        onSecondary = Color.Black,
        secondaryContainer = primary.copy(alpha = 0.15f),
        onSecondaryContainer = Color.White,
        tertiary = primary.copy(alpha = 0.5f),
        onTertiary = Color.Black,
        tertiaryContainer = primary.copy(alpha = 0.1f),
        onTertiaryContainer = Color.White,
        surfaceContainer = Color(0xFF1C1C1C), // 深灰色容器
        onSurface = Color.White,
        onSurfaceVariant = Color.LightGray,
        outline = Color.Gray,
        outlineVariant = Color.DarkGray,
        background = Color.Black, // 严格纯黑
        onBackground = Color.White,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005)
    )
}

/**
 * Wear Material 3 字体排版。
 * 针对手表端小屏幕进行了字号及行高的对齐。
 */
val LXNTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
)

@Composable
fun lxnSwitchButtonColors(): SwitchButtonColors {
    return SwitchButtonDefaults.switchButtonColors(
        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        checkedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        checkedContentColor = MaterialTheme.colorScheme.onSurface,
        uncheckedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        uncheckedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
        uncheckedTrackBorderColor = MaterialTheme.colorScheme.outline
    )
}

@Composable
fun LXNTheme(
    primaryColor: Color = Color(0xFFB7E4C7),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicColorScheme(context) ?: createColorScheme(primaryColor)
    } else {
        createColorScheme(primaryColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LXNTypography,
        content = content
    )
}
