package com.jonesl7l.carinfg.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

private val lightStatusColors =
    StatusColors(
        upToDate = Color(0xFF166534),
        severityLow = Color(0xFF374151),
        severityMedium = Color(0xFF854D0E),
        severityHigh = Color(0xFF9A3412),
        criticalContainer = Color(0xFF991B1B),
        onCritical = Color(0xFFFFFFFF),
        draftContainer = Color(0xFFF3F4F6),
        onDraft = Color(0xFF374151),
        pendingContainer = Color(0xFFE0E8FB),
        onPending = Color(0xFF1E3A8A),
        retryContainer = Color(0xFFFEF3C7),
        onRetry = Color(0xFF78350F),
        attentionContainer = Color(0xFFFFEDD5),
        onAttention = Color(0xFF9A3412),
    )

private val darkStatusColors =
    StatusColors(
        upToDate = Color(0xFF86EFAC),
        severityLow = Color(0xFFC4C9D2),
        severityMedium = Color(0xFFFCD34D),
        severityHigh = Color(0xFFFDBA74),
        criticalContainer = Color(0xFFFCA5A5),
        onCritical = Color(0xFF450A0A),
        draftContainer = Color(0xFF2A2F3A),
        onDraft = Color(0xFFC4C9D2),
        pendingContainer = Color(0xFF1E2A44),
        onPending = Color(0xFFAFC8FB),
        retryContainer = Color(0xFF3A2F14),
        onRetry = Color(0xFFFCD34D),
        attentionContainer = Color(0xFF3B2416),
        onAttention = Color(0xFFFDBA74),
    )

val LocalStatusColors = staticCompositionLocalOf { lightStatusColors }

@Composable
fun CarinfgTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) darkStatusColors else lightStatusColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColors else lightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
