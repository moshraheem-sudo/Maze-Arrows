package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldenHighlight,
    onPrimary = EspressoBrown,
    secondary = DropletBlue,
    onSecondary = Color.White,
    tertiary = EmeraldSuccess,
    background = NightBackground,
    onBackground = NightCreamText,
    surface = NightSurface,
    onSurface = NightCreamText,
    surfaceVariant = Color(0xFF283042),
    onSurfaceVariant = Color(0xFFB8C2D6)
)

private val LightColorScheme = lightColorScheme(
    primary = WarmCocoa,
    onPrimary = Color.White,
    secondary = GoldenHighlight,
    onSecondary = EspressoBrown,
    tertiary = DropletBlue,
    background = CreamBackground,
    onBackground = EspressoBrown,
    surface = CreamSurface,
    onSurface = EspressoBrown,
    surfaceVariant = CreamCard,
    onSurfaceVariant = WarmCocoa
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
