package io.livekit.android.example.voiceassistant.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Blue500,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF070707),
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF131313),
    onSurface = Color(0xFFEEEEEE),
    outline = Color(0xFF202020),
)

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFF9F9F6),
    onBackground = Color(0xFF3B3B3B),
    surface = Color(0xFFF3F3F1),
    onSurface = Color(0xFF3B3B3B),
    outline = Color(0xFFDBDBD8),
)

@Composable
fun LiveKitVoiceAssistantExampleTheme(
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}