package com.jonesl7l.carinfg.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val lightColors =
    lightColorScheme(
        primary = BrandBlue,
        onPrimary = LightSurface,
        primaryContainer = BrandBlueContainer,
        onPrimaryContainer = OnBrandBlueContainer,
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        outlineVariant = LightOutlineVariant,
        error = LightError,
        onError = LightSurface,
        inversePrimary = DarkBrandBlue,
    )

private val darkColors =
    darkColorScheme(
        primary = DarkBrandBlue,
        onPrimary = DarkBackground,
        primaryContainer = DarkBrandBlueContainer,
        onPrimaryContainer = OnDarkBrandBlueContainer,
        background = DarkBackground,
        onBackground = DarkOnSurface,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant,
        error = DarkError,
        onError = OnDarkError,
        inverseSurface = DarkOnSurface,
        inverseOnSurface = LightOnSurface,
        inversePrimary = BrandBlue,
    )

@Composable
fun CarinfgTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        typography = AppTypography,
        content = content,
    )
}
