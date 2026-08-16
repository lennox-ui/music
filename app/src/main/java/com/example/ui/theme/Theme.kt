package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val SpotifyColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    secondary = SpotifyLightGray,
    background = SpotifyBlack,
    surface = SpotifyDarkGray,
    onPrimary = SpotifyBlack,
    onSecondary = SpotifyTextPrimary,
    onBackground = SpotifyTextPrimary,
    onSurface = SpotifyTextPrimary,
    surfaceVariant = SpotifyLightGray,
    onSurfaceVariant = SpotifyTextSecondary
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for Spotify aesthetic
  dynamicColor: Boolean = false, // Disable dynamic colors
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = SpotifyColorScheme, typography = Typography, content = content)
}
