package com.jonesl7l.carinfg.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class StatusColors(
    val severityLow: Color,
    val severityMedium: Color,
    val severityHigh: Color,
    val criticalContainer: Color,
    val onCritical: Color,
    val draftContainer: Color,
    val onDraft: Color,
    val pendingContainer: Color,
    val onPending: Color,
    val retryContainer: Color,
    val onRetry: Color,
    val attentionContainer: Color,
    val onAttention: Color,
)
