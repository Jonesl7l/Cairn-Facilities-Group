package com.jonesl7l.carinfg.core.ui.layout

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

@Composable
fun isCompactHeight(): Boolean = !currentWindowAdaptiveInfo().windowSizeClass.isHeightAtLeastBreakpoint(
    heightDpBreakpoint = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
)
