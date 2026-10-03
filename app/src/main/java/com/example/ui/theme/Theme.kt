package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SignalGreen,
    onPrimary = DeepNavy,
    primaryContainer = CardSurfaceElevated,
    onPrimaryContainer = SignalGreen,
    secondary = SignalCyan,
    onSecondary = DeepNavy,
    secondaryContainer = CardSurfaceElevated,
    onSecondaryContainer = SignalCyan,
    tertiary = SignalPurple,
    onTertiary = Color.White,
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = SignalRed,
    onError = Color.White
)

@Composable
fun NetPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
