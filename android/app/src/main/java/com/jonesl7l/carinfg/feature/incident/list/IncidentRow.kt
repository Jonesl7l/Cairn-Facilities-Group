package com.jonesl7l.carinfg.feature.incident.list

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentListItem
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.model.photo.Photo
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.ui.photo.imageModel
import com.jonesl7l.carinfg.core.ui.theme.LocalStatusColors
import com.jonesl7l.carinfg.feature.incident.statusLabel
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate

private val rowPadding = 16.dp
private val rowVerticalPadding = 12.dp
private val rowSpacing = 12.dp
private val thumbnailSize = 56.dp
private val thumbnailIconSize = 22.dp
private val thumbnailShape = RoundedCornerShape(10.dp)
private val chipShape = RoundedCornerShape(6.dp)
private val chipHeight = 24.dp
private val chipPadding = 8.dp
private val chipIconSize = 14.dp
private val chipSpacing = 8.dp
private val chipIconSpacing = 4.dp
private val chipRowTopPadding = 2.dp
private val chipBorderWidth = 1.dp
private val draftBorderWidth = 1.5.dp
private val lineSpacing = 3.dp
private val titleSize = 16.sp
private val subtitleSize = 13.sp
private val chipTextSize = 12.sp
private val dateFormatter = DateTimeFormatter.ofPattern("d MMM")

@Composable
fun IncidentRow(
    item: IncidentListItem,
    isSelected: Boolean,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDraft = item.syncState == SyncState.DRAFT
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = rowPadding,
                    vertical = rowVerticalPadding,
                ),
        horizontalArrangement = Arrangement.spacedBy(space = rowSpacing),
    ) {
        Thumbnail(
            isDraft = isDraft,
            coverPhoto = item.coverPhoto,
        )
        Column(verticalArrangement = Arrangement.spacedBy(space = lineSpacing)) {
            Text(
                text = item.title,
                fontSize = titleSize,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle(item = item),
                fontSize = subtitleSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ChipRow(
                item = item,
                now = now,
            )
        }
    }
}

@Composable
private fun Thumbnail(
    isDraft: Boolean,
    coverPhoto: Photo?,
) {
    val background = if (isDraft) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier =
            Modifier
                .size(size = thumbnailSize)
                .background(
                    color = background,
                    shape = thumbnailShape,
                ).border(
                    border = BorderStroke(
                        width = draftBorderWidth,
                        color = if (isDraft) MaterialTheme.colorScheme.outline else background,
                    ),
                    shape = thumbnailShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = if (isDraft) R.drawable.ic_edit else R.drawable.ic_image),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size = thumbnailIconSize),
        )
        if (!isDraft && coverPhoto != null) {
            AsyncImage(
                model = coverPhoto.imageModel(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .matchParentSize()
                        .clip(shape = thumbnailShape),
            )
        }
    }
}

@Composable
private fun ChipRow(
    item: IncidentListItem,
    now: Instant,
) {
    val syncChip = IncidentListHelper.syncChip(item = item)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = chipSpacing),
        modifier = Modifier.padding(top = chipRowTopPadding),
    ) {
        val showSeverity = syncChip != SyncChip.DRAFT && syncChip != SyncChip.NEEDS_ATTENTION
        if (showSeverity) item.severity?.let { SeverityChip(severity = it) }
        syncChip?.let { SyncStatusChip(chip = it) }
        Text(
            text =
                timeText(
                    item = item,
                    syncChip = syncChip,
                    now = now,
                ),
            fontSize = chipTextSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun SeverityChip(severity: Severity) {
    val colors = LocalStatusColors.current
    if (severity == Severity.Critical) {
        Chip(
            label = R.string.severity_critical,
            container = colors.criticalContainer,
            content = colors.onCritical,
            border = null,
            icon = null,
        )
        return
    }
    val (label, color) =
        when (severity) {
            Severity.Low -> R.string.severity_low to colors.severityLow
            Severity.Medium -> R.string.severity_medium to colors.severityMedium
            Severity.High -> R.string.severity_high to colors.severityHigh
            else -> R.string.severity_unknown to colors.severityLow
        }
    Chip(
        label = label,
        container = Color.Transparent,
        content = color,
        border = color,
        icon = null,
    )
}

@Composable
private fun SyncStatusChip(chip: SyncChip) {
    val colors = LocalStatusColors.current
    when (chip) {
        SyncChip.DRAFT -> Chip(
            label = R.string.sync_draft,
            container = colors.draftContainer,
            content = colors.onDraft,
            border = null,
            icon = R.drawable.ic_edit,
        )

        SyncChip.PENDING -> Chip(
            label = R.string.sync_pending,
            container = colors.pendingContainer,
            content = colors.onPending,
            border = null,
            icon = R.drawable.ic_clock,
        )

        SyncChip.STILL_TRYING -> Chip(
            label = R.string.sync_still_trying,
            container = colors.retryContainer,
            content = colors.onRetry,
            border = null,
            icon = R.drawable.ic_retry,
        )

        SyncChip.NEEDS_ATTENTION -> Chip(
            label = R.string.sync_needs_attention,
            container = colors.attentionContainer,
            content = colors.onAttention,
            border = null,
            icon = R.drawable.ic_warning,
        )
    }
}

@Composable
private fun Chip(
    @StringRes label: Int,
    container: Color,
    content: Color,
    border: Color?,
    @DrawableRes icon: Int?,
) {
    Row(
        modifier =
            Modifier
                .heightIn(min = chipHeight)
                .background(
                    color = container,
                    shape = chipShape,
                ).border(
                    border = BorderStroke(
                        width = chipBorderWidth,
                        color = border ?: container,
                    ),
                    shape = chipShape,
                ).padding(horizontal = chipPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = chipIconSpacing),
    ) {
        icon?.let {
            Icon(
                painter = painterResource(id = it),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(size = chipIconSize),
            )
        }
        Text(
            text = stringResource(id = label),
            fontSize = chipTextSize,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

@Composable
private fun subtitle(item: IncidentListItem): String {
    val location =
        listOfNotNull(
            item.siteName,
            item.buildingName,
        ).takeIf { it.isNotEmpty() }?.let {
            if (it.size == 1) {
                it.first()
            } else {
                stringResource(
                    id = R.string.location_joined,
                    it[0],
                    it[1],
                )
            }
        }
    val reference =
        when {
            item.syncState == SyncState.DRAFT -> stringResource(id = R.string.not_submitted)
            item.referenceNumber == null -> stringResource(id = R.string.ref_pending)
            else -> item.referenceNumber
        }
    return location?.let {
        stringResource(
            id = R.string.joined_with_dot,
            reference,
            it,
        )
    } ?: reference
}

@Composable
private fun timeText(
    item: IncidentListItem,
    syncChip: SyncChip?,
    now: Instant,
): String {
    val isDraft = syncChip == SyncChip.DRAFT
    val relative =
        relativeTime(
            time = if (isDraft) item.localUpdatedAt else item.occurredAt,
            now = now,
        )
    return when {
        isDraft -> stringResource(id = R.string.time_edited, relative.replaceFirstChar { it.lowercase() })

        syncChip == null -> stringResource(
            id = R.string.joined_with_dot,
            statusLabel(status = item.status),
            relative,
        )

        else -> relative
    }
}

@Composable
private fun relativeTime(
    time: Instant,
    now: Instant,
): String = when (
    val relative =
        RelativeTimeHelper.describe(
            time = time,
            now = now,
            timeZone = TimeZone.currentSystemDefault(),
        )
) {
    RelativeTime.JustNow -> stringResource(id = R.string.time_just_now)
    is RelativeTime.MinutesAgo -> stringResource(id = R.string.time_minutes_ago, relative.minutes)
    is RelativeTime.HoursAgo -> stringResource(id = R.string.time_hours_ago, relative.hours)
    RelativeTime.Yesterday -> stringResource(id = R.string.time_yesterday)
    is RelativeTime.OnDate -> relative.date.toJavaLocalDate().format(dateFormatter)
}
