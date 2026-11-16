package com.jonesl7l.carinfg.core.ui.layout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val maxContentWidth = 640.dp

fun Modifier.contentWidth(): Modifier = this
    .wrapContentWidth(align = Alignment.CenterHorizontally)
    .widthIn(max = maxContentWidth)
    .fillMaxWidth()
