package com.dubiao.yibi.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

val Forest = Color(0xFF153C33)
val ForestSoft = Color(0xFF2F6457)
val Mint = Color(0xFFD8E9DF)
val Cream = Color(0xFFF7F4EC)
val Paper = Color(0xFFFFFCF5)
val Ink = Color(0xFF17211E)
val Muted = Color(0xFF67736F)
val Apricot = Color(0xFFF4C98B)
val Coral = Color(0xFFE17966)
val Hairline = Color(0xFFE4E5DE)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = Forest,
    secondary = ForestSoft,
    tertiary = Coral,
    background = Cream,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0EFE8),
    onSurfaceVariant = Muted,
    outline = Hairline,
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8D5C2),
    onPrimary = Color(0xFF08372C),
    primaryContainer = Forest,
    secondary = Color(0xFFA8D5C2),
    tertiary = Apricot,
    background = Color(0xFF101815),
    surface = Color(0xFF17211E),
    onSurface = Color(0xFFF1F3EE),
)

@Composable
fun YiBiTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = if (darkTheme) DarkColors else LightColors
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = scheme, typography = YiBiTypography, content = content)
}
