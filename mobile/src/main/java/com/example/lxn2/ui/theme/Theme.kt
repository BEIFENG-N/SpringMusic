package com.example.lxn2.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SpringDarkPrimary,
    secondary = SpringDarkSecondary,
    tertiary = SpringDarkTertiary,
    background = Color(0xFF101411),
    surface = Color(0xFF101411),
    onPrimary = Color(0xFF00391F),
    onSecondary = Color(0xFF00391F),
    onBackground = Color(0xFFE1E3DF),
    onSurface = Color(0xFFE1E3DF)
)

private val LightColorScheme = lightColorScheme(
    primary = SpringPrimary,
    secondary = SpringSecondary,
    tertiary = SpringTertiary,
    background = Color(0xFFF7FBF4),
    surface = Color(0xFFF7FBF4),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A)
)

@Composable
fun LXNTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}