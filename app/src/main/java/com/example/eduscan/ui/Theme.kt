package com.example.eduscan.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5E4BB7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFF2E7D6B),
    background = Color(0xFFFCFBFF),
    surface = Color(0xFFFCFBFF)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFC9BEFF),
    onPrimary = Color(0xFF2B1E6B),
    primaryContainer = Color(0xFF443A8A),
    secondary = Color(0xFF7FD6BF),
    background = Color(0xFF121216),
    surface = Color(0xFF121216)
)

@Composable
fun EduScanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content
    )
}
