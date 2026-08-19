package com.aistudio.mcpchat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AmoledDarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = SlateSurfaceDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = IndigoPrimaryLight,
    secondary = CyanSecondaryLight,
    onSecondary = SlateSurfaceDark,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanSecondaryLight,
    tertiary = EmeraldTertiaryLight,
    onTertiary = SlateSurfaceDark,
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = EmeraldTertiaryLight,
    background = Color.Black,
    onBackground = SlateTextPrimaryDark,
    surface = Color.Black,
    onSurface = SlateTextPrimaryDark,
    surfaceVariant = Color(0xFF0A0A0A),
    onSurfaceVariant = SlateTextSecondaryDark,
    outline = SlateBorderDark,
    error = RoseError,
    errorContainer = RoseErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = SlateSurfaceDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = IndigoPrimaryLight,
    secondary = CyanSecondaryLight,
    onSecondary = SlateSurfaceDark,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanSecondaryLight,
    tertiary = EmeraldTertiaryLight,
    onTertiary = SlateSurfaceDark,
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = EmeraldTertiaryLight,
    background = SlateBackgroundDark,
    onBackground = SlateTextPrimaryDark,
    surface = SlateSurfaceDark,
    onSurface = SlateTextPrimaryDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = SlateTextSecondaryDark,
    outline = SlateBorderDark,
    error = RoseError,
    errorContainer = RoseErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = SlateSurfaceLight,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = IndigoPrimaryDark,
    secondary = CyanSecondary,
    onSecondary = SlateSurfaceLight,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = CyanSecondary,
    tertiary = EmeraldTertiary,
    onTertiary = SlateSurfaceLight,
    tertiaryContainer = EmeraldContainerLight,
    onTertiaryContainer = EmeraldTertiary,
    background = SlateBackgroundLight,
    onBackground = SlateTextPrimaryLight,
    surface = SlateSurfaceLight,
    onSurface = SlateTextPrimaryLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = SlateTextSecondaryLight,
    outline = SlateBorderLight,
    error = RoseError,
    errorContainer = RoseErrorContainer
)

object ThemePrefs {
    var useAmoledDark by mutableStateOf(false)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme && ThemePrefs.useAmoledDark -> AmoledDarkColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
