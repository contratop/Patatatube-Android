package com.patatatube.mobile.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    secondary = md_theme_light_secondary,
    background = md_theme_light_background,
    surface = md_theme_light_surface,
    onBackground = md_theme_light_onBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    secondary = md_theme_dark_secondary,
    background = md_theme_dark_background,
    surface = md_theme_dark_surface,
    onBackground = md_theme_dark_onBackground
)

private val PokeColorScheme = lightColorScheme(
    primary = poke_theme_primary,
    onPrimary = poke_theme_onPrimary,
    secondary = poke_theme_secondary,
    background = poke_theme_background,
    surface = poke_theme_surface,
    onBackground = poke_theme_onBackground
)

enum class AppThemeMode {
    LIGHT, DARK, POKE
}

@Composable
fun PatatatubeMobileTheme(
    appThemeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val targetColorScheme = when (appThemeMode) {
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.POKE -> PokeColorScheme
    }

    val primary by animateColorAsState(targetColorScheme.primary, tween(500))
    val onPrimary by animateColorAsState(targetColorScheme.onPrimary, tween(500))
    val secondary by animateColorAsState(targetColorScheme.secondary, tween(500))
    val background by animateColorAsState(targetColorScheme.background, tween(500))
    val surface by animateColorAsState(targetColorScheme.surface, tween(500))
    val onBackground by animateColorAsState(targetColorScheme.onBackground, tween(500))

    val colorScheme = androidx.compose.material3.ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = targetColorScheme.primaryContainer,
        onPrimaryContainer = targetColorScheme.onPrimaryContainer,
        inversePrimary = targetColorScheme.inversePrimary,
        secondary = secondary,
        onSecondary = targetColorScheme.onSecondary,
        secondaryContainer = targetColorScheme.secondaryContainer,
        onSecondaryContainer = targetColorScheme.onSecondaryContainer,
        tertiary = targetColorScheme.tertiary,
        onTertiary = targetColorScheme.onTertiary,
        tertiaryContainer = targetColorScheme.tertiaryContainer,
        onTertiaryContainer = targetColorScheme.onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = targetColorScheme.onSurface,
        surfaceVariant = targetColorScheme.surfaceVariant,
        onSurfaceVariant = targetColorScheme.onSurfaceVariant,
        surfaceTint = targetColorScheme.surfaceTint,
        inverseSurface = targetColorScheme.inverseSurface,
        inverseOnSurface = targetColorScheme.inverseOnSurface,
        error = targetColorScheme.error,
        onError = targetColorScheme.onError,
        errorContainer = targetColorScheme.errorContainer,
        onErrorContainer = targetColorScheme.onErrorContainer,
        outline = targetColorScheme.outline,
        outlineVariant = targetColorScheme.outlineVariant,
        scrim = targetColorScheme.scrim
    )
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = targetColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = 
                appThemeMode == AppThemeMode.LIGHT || appThemeMode == AppThemeMode.POKE
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
