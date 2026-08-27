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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val amoledDarkColorScheme = darkColorScheme(
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
    surfaceVariant = Color(0xFF111827),
    onSurfaceVariant = SlateTextSecondaryDark,
    outline = SlateBorderDark,
    error = RoseError,
    errorContainer = RoseErrorContainer,
)

private val darkColorScheme = darkColorScheme(
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
    errorContainer = RoseErrorContainer,
)

private val lightColorScheme = lightColorScheme(
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
    errorContainer = RoseErrorContainer,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    useAmoledDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme && useAmoledDark -> amoledDarkColorScheme
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val viewContext = view.context as? Activity ?: return@SideEffect
            WindowCompat.getInsetsController(viewContext.window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
