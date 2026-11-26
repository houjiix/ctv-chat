package com.cinematv.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CinemaDark = darkColorScheme(
    primary = Color(0xFFE7B85A),
    onPrimary = Color(0xFF211807),
    secondary = Color(0xFFC5A66B),
    background = Color(0xFF090B10),
    surface = Color(0xFF141720),
    surfaceVariant = Color(0xFF222733),
    onBackground = Color(0xFFF3F3F1),
    onSurface = Color(0xFFF3F3F1),
    onSurfaceVariant = Color(0xFFB6BAC4)
)

private val CinemaBlack = CinemaDark.copy(
    background = Color.Black,
    surface = Color(0xFF080808),
    surfaceVariant = Color(0xFF171717)
)

private val CinemaLight = lightColorScheme(
    primary = Color(0xFF76520D),
    onPrimary = Color.White,
    secondary = Color(0xFF775E2C),
    background = Color(0xFFF7F5F0),
    surface = Color.White,
    surfaceVariant = Color(0xFFEAE6DD),
    onBackground = Color(0xFF1C1B18),
    onSurface = Color(0xFF1C1B18),
    onSurfaceVariant = Color(0xFF59564F)
)

val LocalReducedMotion = staticCompositionLocalOf { true }

@Composable
fun CinemaTheme(theme: String, reducedMotion: Boolean, content: @Composable () -> Unit) {
    val colors = when (theme) {
        "black" -> CinemaBlack
        "light" -> CinemaLight
        else -> CinemaDark
    }
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalReducedMotion provides reducedMotion) { content() }
    }
}
