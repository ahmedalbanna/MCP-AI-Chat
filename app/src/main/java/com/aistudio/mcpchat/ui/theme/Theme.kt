package com.aistudio.mcpchat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.cornerRadiusSm),
    small = RoundedCornerShape(Dimens.cornerRadiusMd),
    medium = RoundedCornerShape(Dimens.cornerRadiusLg),
    large = RoundedCornerShape(Dimens.cornerRadiusXl),
    extraLarge = RoundedCornerShape(Dimens.cornerRadiusXl),
)

private val amoledDarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = SlateSurfaceContainerLowDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = IndigoPrimaryLight,
    secondary = CyanSecondaryLight,
    onSecondary = SlateSurfaceContainerLowDark,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanSecondaryLight,
    tertiary = EmeraldTertiaryLight,
    onTertiary = SlateSurfaceContainerLowDark,
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = EmeraldTertiaryLight,
    background = Color.Black,
    onBackground = SlateTextPrimaryDark,
    surface = Color.Black,
    onSurface = SlateTextPrimaryDark,
    surfaceVariant = SlateSurfaceContainerDark,
    onSurfaceVariant = SlateTextSecondaryDark,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = SlateSurfaceContainerLowestDark,
    surfaceContainer = SlateSurfaceContainerDark,
    surfaceContainerHigh = SlateSurfaceContainerHighDark,
    surfaceContainerHighest = SlateSurfaceHighlightDark,
    surfaceTint = IndigoPrimaryLight,
    inverseSurface = SlateTextPrimaryDark,
    inverseOnSurface = SlateSurfaceContainerLowDark,
    inversePrimary = IndigoPrimaryDark,
    outline = SlateBorderDark,
    outlineVariant = SlateBorderVariantDark,
    scrim = Color.Black,
    error = RoseError,
    onError = SlateSurfaceContainerLowDark,
    errorContainer = RoseErrorContainerDark,
    onErrorContainer = RoseErrorContainerLight,
)

private val darkColorScheme = darkColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = SlateSurfaceContainerLowDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = IndigoPrimaryLight,
    secondary = CyanSecondaryLight,
    onSecondary = SlateSurfaceContainerLowDark,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanSecondaryLight,
    tertiary = EmeraldTertiaryLight,
    onTertiary = SlateSurfaceContainerLowDark,
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = EmeraldTertiaryLight,
    background = SlateBackgroundDark,
    onBackground = SlateTextPrimaryDark,
    surface = SlateSurfaceDark,
    onSurface = SlateTextPrimaryDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = SlateTextSecondaryDark,
    surfaceContainerLowest = SlateSurfaceContainerLowestDark,
    surfaceContainerLow = SlateSurfaceContainerLowDark,
    surfaceContainer = SlateSurfaceContainerDark,
    surfaceContainerHigh = SlateSurfaceContainerHighDark,
    surfaceContainerHighest = SlateSurfaceHighlightDark,
    surfaceTint = IndigoPrimaryLight,
    inverseSurface = SlateTextPrimaryDark,
    inverseOnSurface = SlateSurfaceContainerLowDark,
    inversePrimary = IndigoPrimaryDark,
    outline = SlateBorderDark,
    outlineVariant = SlateBorderVariantDark,
    scrim = Color.Black,
    error = RoseError,
    onError = SlateSurfaceContainerLowDark,
    errorContainer = RoseErrorContainerDark,
    onErrorContainer = RoseErrorContainerLight,
)

private val lightColorScheme = lightColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = SlateSurfaceLight,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = IndigoPrimaryDark,
    secondary = CyanTextOnLight,
    onSecondary = SlateSurfaceLight,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = CyanTextOnLight,
    tertiary = EmeraldTextOnLight,
    onTertiary = SlateSurfaceLight,
    tertiaryContainer = EmeraldContainerLight,
    onTertiaryContainer = EmeraldTextOnLight,
    background = SlateBackgroundLight,
    onBackground = SlateTextPrimaryLight,
    surface = SlateSurfaceLight,
    onSurface = SlateTextPrimaryLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = SlateTextSecondaryLight,
    surfaceContainerLowest = SlateSurfaceContainerLowestLight,
    surfaceContainerLow = SlateSurfaceContainerLowLight,
    surfaceContainer = SlateSurfaceContainerLight,
    surfaceContainerHigh = SlateSurfaceContainerHighLight,
    surfaceContainerHighest = SlateSurfaceContainerHighestLight,
    surfaceTint = IndigoPrimaryDark,
    inverseSurface = SlateTextPrimaryLight,
    inverseOnSurface = SlateSurfaceContainerLowLight,
    inversePrimary = IndigoPrimaryLight,
    outline = SlateBorderLight,
    outlineVariant = SlateBorderVariantLight,
    scrim = Color.Black,
    error = RoseErrorOnLight,
    onError = SlateSurfaceLight,
    errorContainer = RoseErrorContainerLight,
    onErrorContainer = RoseErrorContainerDark,
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
            val controller = WindowCompat.getInsetsController(viewContext.window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
