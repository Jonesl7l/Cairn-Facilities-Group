package com.jonesl7l.carinfg.feature.incident.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentDetail
import com.jonesl7l.carinfg.core.domain.model.photo.Photo
import com.jonesl7l.carinfg.core.ui.photo.imageModel
import com.jonesl7l.carinfg.feature.incident.CoverBadge
import com.jonesl7l.carinfg.feature.incident.actionLabel
import com.jonesl7l.carinfg.feature.incident.severityLabel
import com.jonesl7l.carinfg.feature.incident.statusLabel

private val bodyHorizontalPadding = 28.dp
private val bodyVerticalPadding = 24.dp
private val blockSpacing = 20.dp
private val titleSpacing = 10.dp
private val chipSpacing = 8.dp
private val gridSpacing = 16.dp
private val photoSpacing = 12.dp
private val photoHeight = 170.dp
private val photoShape = RoundedCornerShape(14.dp)
private val photoIconSize = 34.dp
private val titleSize = 30.sp
private val reportedSize = 14.sp
private const val PHOTO_COLUMNS = 3
private const val SECTION_COLUMNS = 2

@Composable
internal fun WideDetailBody(
    uiState: IncidentDetailUiState,
    detail: IncidentDetail,
    onResolve: (Resolution) -> Unit,
) {
    val incident = detail.incident
    val sections = sectionsFor(detail = detail)
    Column(
        verticalArrangement = Arrangement.spacedBy(space = blockSpacing),
        modifier =
            Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(
                    horizontal = bodyHorizontalPadding,
                    vertical = bodyVerticalPadding,
                ),
    ) {
        TitleBlock(
            uiState = uiState,
            detail = detail,
        )
        if (uiState.needsAttention) {
            AttentionNote(
                reason = incident.syncStatus.conflictReason,
                isLockedDelete = uiState.isLockedDelete,
                onResolve = onResolve,
            )
        }
        if (uiState.isLocked) LockedNote()
        if (incident.photos.isNotEmpty()) PhotoGrid(photos = incident.photos)
        sections.chunked(size = SECTION_COLUMNS).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = gridSpacing),
                modifier = Modifier.height(intrinsicSize = IntrinsicSize.Min),
            ) {
                row.forEach { section ->
                    section(
                        Modifier
                            .weight(weight = 1f)
                            .fillMaxHeight(),
                    )
                }
                repeat(times = SECTION_COLUMNS - row.size) { Spacer(modifier = Modifier.weight(weight = 1f)) }
            }
        }
    }
}

private fun sectionsFor(detail: IncidentDetail): List<@Composable (Modifier) -> Unit> {
    val incident = detail.incident
    return buildList {
        add { modifier ->
            DetailsSection(
                incident = incident,
                isCard = true,
                modifier = modifier,
            )
        }
        add { modifier ->
            LocationSection(
                incident = incident,
                names = detail.names,
                isCard = true,
                modifier = modifier,
            )
        }
        add { modifier ->
            SafetySection(
                incident = incident,
                isCard = true,
                modifier = modifier,
            )
        }
        if (incident.safety.immediateActions.isNotEmpty()) {
            add { modifier ->
                ImmediateActionsSection(
                    incident = incident,
                    modifier = modifier,
                )
            }
        }
        if (incident.persons.isNotEmpty()) {
            add { modifier ->
                PeopleSection(
                    incident = incident,
                    isCard = true,
                    modifier = modifier,
                )
            }
        }
        if (!incident.additionalNotes.isNullOrBlank()) {
            add { modifier ->
                NotesSection(
                    incident = incident,
                    modifier = modifier,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleBlock(
    uiState: IncidentDetailUiState,
    detail: IncidentDetail,
) {
    val incident = detail.incident
    Column(verticalArrangement = Arrangement.spacedBy(space = titleSpacing)) {
        Text(
            text = incident.details.title,
            fontSize = titleSize,
            fontWeight = FontWeight.SemiBold,
            lineHeight = titleSize,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(space = chipSpacing),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Chip(
                text = stringResource(id = R.string.status_chip, statusLabel(status = incident.operatorFields.status)),
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurface,
            )
            incident.details.severity?.let {
                Chip(
                    text = stringResource(id = R.string.severity_chip, severityLabel(severity = it)),
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text =
                    reportedLine(
                        incident = incident,
                        names = detail.names,
                        isMine = uiState.isMine,
                    ),
                fontSize = reportedSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PhotoGrid(photos: List<Photo>) {
    Column(verticalArrangement = Arrangement.spacedBy(space = photoSpacing)) {
        photos.withIndex().chunked(size = PHOTO_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(space = photoSpacing)) {
                row.forEach { (index, photo) ->
                    PhotoTile(
                        photo = photo,
                        description = stringResource(
                            id = if (index == 0) R.string.photo_cover_description else R.string.photo_description,
                            index + 1,
                            photos.size,
                        ),
                        isCover = index == 0,
                        modifier = Modifier.weight(weight = 1f),
                    )
                }
                repeat(times = PHOTO_COLUMNS - row.size) { Spacer(modifier = Modifier.weight(weight = 1f)) }
            }
        }
    }
}

@Composable
private fun PhotoTile(
    photo: Photo,
    description: String,
    isCover: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .height(height = photoHeight)
                .clip(shape = photoShape)
                .background(color = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_image),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size = photoIconSize),
        )
        AsyncImage(
            model = photo.imageModel(),
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isCover) CoverBadge(modifier = Modifier.align(alignment = Alignment.BottomStart))
    }
}

@Composable
private fun ImmediateActionsSection(
    incident: Incident,
    modifier: Modifier = Modifier,
) {
    Section(
        title = R.string.label_immediate_actions,
        isCard = true,
        modifier = modifier,
    ) {
        Text(
            text = incident.safety.immediateActions.map { actionLabel(action = it) }.joinToString(separator = " · "),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NotesSection(
    incident: Incident,
    modifier: Modifier = Modifier,
) {
    Section(
        title = R.string.section_notes,
        isCard = true,
        modifier = modifier,
    ) {
        Text(
            text = incident.additionalNotes.orEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
