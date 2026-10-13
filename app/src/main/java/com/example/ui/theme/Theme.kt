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

private val DarkColorScheme = darkColorScheme(
    primary = GitHubGreenLight,
    onPrimary = GitHubBgDark,
    primaryContainer = GitHubGreen,
    onPrimaryContainer = GitHubTextPrimaryDark,
    secondary = GitHubBlue,
    onSecondary = GitHubBgDark,
    secondaryContainer = GitHubSurfaceDark,
    onSecondaryContainer = GitHubBlue,
    tertiary = GitHubPurple,
    onTertiary = GitHubBgDark,
    background = GitHubBgDark,
    onBackground = GitHubTextPrimaryDark,
    surface = GitHubSurfaceDark,
    onSurface = GitHubTextPrimaryDark,
    surfaceVariant = GitHubCardDark,
    onSurfaceVariant = GitHubTextSecondaryDark,
    outline = GitHubBorderDark,
    error = GitHubRed
)

private val LightColorScheme = lightColorScheme(
    primary = GitHubGreenDark,
    onPrimary = GitHubSurfaceLight,
    primaryContainer = GitHubCardLight,
    onPrimaryContainer = GitHubGreenDark,
    secondary = GitHubBlueDark,
    onSecondary = GitHubSurfaceLight,
    secondaryContainer = GitHubCardLight,
    onSecondaryContainer = GitHubBlueDark,
    tertiary = GitHubPurple,
    onTertiary = GitHubSurfaceLight,
    background = GitHubBgLight,
    onBackground = GitHubTextPrimaryLight,
    surface = GitHubSurfaceLight,
    onSurface = GitHubTextPrimaryLight,
    surfaceVariant = GitHubCardLight,
    onSurfaceVariant = GitHubTextSecondaryLight,
    outline = GitHubBorderLight,
    error = GitHubRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep developer GitHub theme consistent
    content: @Composable () -> Unit,
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
