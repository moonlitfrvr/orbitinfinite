package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OrbitColorScheme = lightColorScheme(
    primary = OrbitPrimary,
    secondary = OrbitSecondary,
    tertiary = OrbitAccent,
    background = OrbitBackground,
    surface = OrbitCard,
    onPrimary = Color.White,
    onSecondary = OrbitText,
    onTertiary = OrbitText,
    onBackground = OrbitText,
    onSurface = OrbitText
)

@Composable
fun OrbitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OrbitColorScheme,
        typography = Typography,
        content = content
    )
}
