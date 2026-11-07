package com.example.workout

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

/** Plain surfaces, grey text and one green accent taken from the app icon. */
@Composable
fun WorkoutTheme(content: @Composable () -> Unit) {
    val choice by Storage.theme.collectAsState()
    val dark = when (choice) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFF6BE04F),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFF9A9A9A),
            tertiary = Color(0xFF9A9A9A),
            background = Color(0xFF000000),
            onBackground = Color(0xFFEDEDED),
            surface = Color(0xFF000000),
            onSurface = Color(0xFFEDEDED),
            surfaceVariant = Color(0xFF161616),
            onSurfaceVariant = Color(0xFF9A9A9A),
            outline = Color(0xFF2A2A2A),
            outlineVariant = Color(0xFF242424)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1E8E3E),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF6B6B6B),
            tertiary = Color(0xFF6B6B6B),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF111111),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF111111),
            surfaceVariant = Color(0xFFF3F3F3),
            onSurfaceVariant = Color(0xFF6B6B6B),
            outline = Color(0xFFD9D9D9),
            outlineVariant = Color(0xFFE6E6E6)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
