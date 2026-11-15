package com.jonesl7l.carinfg.feature.incident.list

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.sync.status.AppSyncStatus
import com.jonesl7l.carinfg.core.sync.status.SyncIndicator
import com.jonesl7l.carinfg.core.ui.theme.LocalStatusColors

private val pillHeight = 44.dp
private val pillPadding = 12.dp
private val pillSpacing = 6.dp
private val pillBorderWidth = 1.dp
private val iconSize = 18.dp
private val dividerWidth = 1.dp
private val dividerHeight = 18.dp
private val countTextSize = 14.sp
private const val SPIN_DURATION_MILLIS = 1000
private const val FULL_TURN = 360f

@Composable
fun SyncStatusPill(
    status: AppSyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStatusColors.current
    val indicator = status.indicator
    val isAttention = indicator is SyncIndicator.NeedsAttention
    val description =
        stringResource(
            id = R.string.status_description,
            stringResource(id = if (status.isOnline) R.string.status_online else R.string.status_offline),
            indicatorLabel(indicator = indicator),
        )
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isAttention) colors.attentionContainer else MaterialTheme.colorScheme.surface,
        border =
            BorderStroke(
                width = pillBorderWidth,
                color = if (isAttention) colors.onAttention else MaterialTheme.colorScheme.outlineVariant,
            ),
        modifier =
            modifier
                .heightIn(min = pillHeight)
                .semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = pillPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = pillSpacing),
        ) {
            PillIcon(
                icon = if (status.isOnline) R.drawable.ic_signal else R.drawable.ic_signal_off,
                tint = if (status.isOnline) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier =
                    Modifier
                        .size(
                            width = dividerWidth,
                            height = dividerHeight,
                        ).background(color = MaterialTheme.colorScheme.outlineVariant),
            )
            IndicatorIcon(indicator = indicator)
        }
    }
}

@Composable
private fun IndicatorIcon(indicator: SyncIndicator) {
    val colors = LocalStatusColors.current
    when (indicator) {
        SyncIndicator.UpToDate -> PillIcon(
            icon = R.drawable.ic_check_circle,
            tint = colors.upToDate,
        )

        SyncIndicator.Syncing -> SpinningIcon()

        is SyncIndicator.Waiting -> {
            PillIcon(
                icon = R.drawable.ic_cloud_upload,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Count(
                count = indicator.count,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        is SyncIndicator.NeedsAttention -> {
            PillIcon(
                icon = R.drawable.ic_warning,
                tint = colors.onAttention,
            )
            Count(
                count = indicator.count,
                color = colors.onAttention,
            )
        }
    }
}

@Composable
private fun SpinningIcon() {
    val rotation by rememberInfiniteTransition(label = "sync").animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = SPIN_DURATION_MILLIS,
                        easing = LinearEasing,
                    ),
                repeatMode = RepeatMode.Restart,
            ),
        label = "rotation",
    )
    PillIcon(
        icon = R.drawable.ic_retry,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.rotate(degrees = rotation),
    )
}

@Composable
private fun PillIcon(
    @DrawableRes icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(id = icon),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size = iconSize),
    )
}

@Composable
private fun Count(
    count: Int,
    color: Color,
) {
    Text(
        text = count.toString(),
        color = color,
        fontSize = countTextSize,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun indicatorLabel(indicator: SyncIndicator): String = when (indicator) {
    SyncIndicator.UpToDate -> stringResource(id = R.string.status_up_to_date)
    SyncIndicator.Syncing -> stringResource(id = R.string.status_syncing)
    is SyncIndicator.Waiting -> stringResource(id = R.string.status_waiting, indicator.count)
    is SyncIndicator.NeedsAttention -> stringResource(id = R.string.status_needs_attention, indicator.count)
}
