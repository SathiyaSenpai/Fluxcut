package com.android.fluxcut.ui.theme

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
    primary = FluxAccent,
    secondary = FluxAmber,
    tertiary = FluxGreen,
    surface = FluxBg,
    onSurface = Color.White,
    surfaceVariant = FluxSurface2,
    onSurfaceVariant = FluxSubtle,
    outline = FluxDivider,
    error = FluxErr,
    errorContainer = FluxRose,
    primaryContainer = FluxSky,
    surfaceContainer = FluxSurface
)

private val LightColorScheme = lightColorScheme(
    primary = FluxAccent,
    secondary = FluxAmber,
    tertiary = FluxGreen,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F7),
    onSurfaceVariant = Color(0xFF888888),
    outline = Color(0xFFEEEEF5)
)

@Composable
fun FluxcutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent branding
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
