package com.livesense.japanese.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = LiveSensePrimary,
    background = LiveSenseBackground,
    surfaceVariant = LiveSenseSurface,
)

@Composable
fun LiveSenseJapaneseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = LiveSenseTypography,
        content = content,
    )
}
