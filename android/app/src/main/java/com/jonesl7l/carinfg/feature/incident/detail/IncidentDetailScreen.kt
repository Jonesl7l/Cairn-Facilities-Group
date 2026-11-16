package com.jonesl7l.carinfg.feature.incident.detail

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentDetail
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentNames
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.ui.theme.LocalStatusColors
import com.jonesl7l.carinfg.feature.incident.roleLabel
import com.jonesl7l.carinfg.feature.incident.severityLabel
import com.jonesl7l.carinfg.feature.incident.statusLabel
import com.jonesl7l.carinfg.feature.incident.typeLabel
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime

private val sectionSpacing = 10.dp
private val sectionPadding = 20.dp
private val sectionVerticalPadding = 16.dp
private val itemSpacing = 12.dp
private val labelSpacing = 2.dp
private val chipShape = RoundedCornerShape(8.dp)
private val chipPadding = 10.dp
private val chipHeight = 28.dp
private val noteShape = RoundedCornerShape(12.dp)
private val notePadding = 14.dp
private val noteMargin = 16.dp
private val buttonShape = RoundedCornerShape(14.dp)
private val buttonHeight = 56.dp
private val iconSize = 20.dp
private val referenceTextSize = 13.sp
private val labelTextSize = 12.sp
private val valueTextSize = 15.sp
private val headerTextSize = 13.sp
private val occurredFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")
private val reportedFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

@Composable
fun IncidentDetailScreen(
    incidentId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteFinish: (DeleteOutcome) -> Unit,
    viewModel: IncidentDetailViewModel =
        hiltViewModel<IncidentDetailViewModel, IncidentDetailViewModel.Factory>(
            key = incidentId,
            creationCallback = { factory -> factory.create(incidentId = incidentId) },
        ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDeleteFinish by rememberUpdatedState(onDeleteFinish)
    LaunchedEffect(uiState.deleteOutcome) { uiState.deleteOutcome?.let { currentOnDeleteFinish(it) } }
    IncidentDetailContent(
        uiState = uiState,
        onBack = onBack,
        onEdit = onEdit,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailContent(
    uiState: IncidentDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = stringResource(id = R.string.back),
                        )
                    }
                },
                actions = { if (uiState.canDelete) OverflowMenu(onDelete = onDelete) },
            )
        },
        bottomBar = { if (uiState.canEdit) EditBar(onEdit = onEdit) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues = innerPadding),
        ) {
            val detail = uiState.detail
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(alignment = Alignment.Center))

                detail == null -> Text(
                    text = stringResource(id = R.string.incident_not_found),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .align(alignment = Alignment.Center)
                            .padding(all = sectionPadding),
                )

                else -> DetailBody(
                    uiState = uiState,
                    detail = detail,
                )
            }
        }
    }
}

@Composable
private fun DetailBody(
    uiState: IncidentDetailUiState,
    detail: IncidentDetail,
) {
    val incident = detail.incident
    Column(
        modifier = Modifier.verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = sectionSpacing),
    ) {
        Header(
            incident = incident,
            names = detail.names,
            isMine = uiState.isMine,
        )
        if (uiState.needsAttention) AttentionNote(reason = incident.syncStatus.conflictReason)
        if (uiState.isLocked) LockedNote()
        DetailsSection(incident = incident)
        LocationSection(
            incident = incident,
            names = detail.names,
        )
        SafetySection(incident = incident)
        if (incident.persons.isNotEmpty()) PeopleSection(incident = incident)
    }
}

@Composable
private fun Header(
    incident: Incident,
    names: IncidentNames,
    isMine: Boolean,
) {
    Section {
        Text(
            text = incident.referenceNumber ?: stringResource(id = R.string.ref_pending),
            fontSize = referenceTextSize,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = incident.details.title,
            style = MaterialTheme.typography.titleLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(space = itemSpacing)) {
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
        }
        Text(
            text =
                reportedLine(
                    incident = incident,
                    names = names,
                    isMine = isMine,
                ),
            fontSize = referenceTextSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailsSection(incident: Incident) {
    Section(title = R.string.section_details) {
        Row {
            LabelledValue(
                label = R.string.label_type,
                value = incident.details.type?.let { typeLabel(type = it) } ?: stringResource(id = R.string.value_not_set),
                modifier = Modifier.weight(weight = 1f),
            )
            LabelledValue(
                label = R.string.label_occurred,
                value =
                    format(
                        instant = incident.details.occurredAt,
                        timeZone = incident.details.occurredTimeZone,
                        formatter = occurredFormatter,
                    ),
                modifier = Modifier.weight(weight = 1f),
            )
        }
        if (incident.details.description.isNotBlank()) {
            Text(
                text = incident.details.description,
                fontSize = valueTextSize,
            )
        }
    }
}

@Composable
private fun LocationSection(
    incident: Incident,
    names: IncidentNames,
) {
    val location = incident.location
    val unknown = stringResource(id = R.string.value_unknown)
    val path =
        listOfNotNull(
            location.siteId?.let { names.siteName ?: unknown },
            location.buildingId?.let { names.buildingName ?: unknown },
            location.floorId?.let { names.floorName ?: unknown },
        ).joinToString(separator = stringResource(id = R.string.location_separator))
    val spot =
        listOfNotNull(
            location.roomId?.let { names.roomName ?: unknown } ?: location.unlistedLocation,
            location.exactSpot,
        ).joinToString(separator = ", ")
    Section(title = R.string.section_location) {
        Row(horizontalArrangement = Arrangement.spacedBy(space = itemSpacing)) {
            NoteIcon(
                icon = R.drawable.ic_pin,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(space = labelSpacing)) {
                Text(
                    text = path.ifBlank { stringResource(id = R.string.value_not_set) },
                    fontSize = valueTextSize,
                    fontWeight = FontWeight.Medium,
                )
                if (spot.isNotBlank()) {
                    Text(
                        text = spot,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (location.assetId != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = noteShape,
                        ).padding(all = itemSpacing),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text =
                        stringResource(
                            id = R.string.asset_label,
                            names.assetCode.orEmpty(),
                            names.assetName ?: unknown,
                        ),
                )
                if (location.equipmentOutOfService) {
                    Text(
                        text = stringResource(id = R.string.out_of_service),
                        fontWeight = FontWeight.SemiBold,
                        color = LocalStatusColors.current.onAttention,
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetySection(incident: Incident) {
    Section(title = R.string.section_safety) {
        Row {
            LabelledValue(
                label = R.string.label_injury,
                value = yesNo(value = incident.safety.injuryOccurred),
                modifier = Modifier.weight(weight = 1f),
            )
            LabelledValue(
                label = R.string.label_hazard,
                value = yesNo(value = incident.safety.hazardStillPresent),
                modifier = Modifier.weight(weight = 1f),
            )
        }
    }
}

@Composable
private fun PeopleSection(incident: Incident) {
    Section(title = R.string.section_people) {
        incident.persons.forEach { person ->
            LabelledValue(
                label = null,
                value = person.name,
                detail = person.role?.let { roleLabel(role = it) } ?: stringResource(id = R.string.value_unknown),
            )
        }
    }
}

@Composable
private fun AttentionNote(reason: ConflictReason?) {
    val colors = LocalStatusColors.current
    Note(
        icon = R.drawable.ic_warning,
        title = R.string.attention_title,
        body =
            when (reason) {
                ConflictReason.DELETED_REMOTELY -> R.string.attention_deleted_remotely
                ConflictReason.STATUS_LOCKED -> R.string.attention_status_locked
                ConflictReason.VALIDATION, null -> R.string.attention_validation
            },
        container = colors.attentionContainer,
        content = colors.onAttention,
    )
}

@Composable
private fun LockedNote() {
    Note(
        icon = R.drawable.ic_lock,
        title = R.string.locked_title,
        body = R.string.locked_body,
        container = MaterialTheme.colorScheme.primaryContainer,
        content = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@Composable
private fun Note(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes body: Int,
    container: Color,
    content: Color,
) {
    Row(
        modifier =
            Modifier
                .padding(horizontal = noteMargin)
                .fillMaxWidth()
                .background(
                    color = container,
                    shape = noteShape,
                ).padding(all = notePadding),
        horizontalArrangement = Arrangement.spacedBy(space = itemSpacing),
    ) {
        NoteIcon(
            icon = icon,
            tint = content,
        )
        Column(verticalArrangement = Arrangement.spacedBy(space = labelSpacing)) {
            Text(
                text = stringResource(id = title),
                fontWeight = FontWeight.SemiBold,
                color = content,
            )
            Text(
                text = stringResource(id = body),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun NoteIcon(
    @DrawableRes icon: Int,
    tint: Color,
) {
    Icon(
        painter = painterResource(id = icon),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size = iconSize),
    )
}

@Composable
private fun Section(
    @StringRes title: Int? = null,
    content: @Composable () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = sectionPadding,
                        vertical = sectionVerticalPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(space = itemSpacing),
        ) {
            title?.let {
                Text(
                    text = stringResource(id = it).uppercase(),
                    fontSize = headerTextSize,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun LabelledValue(
    @StringRes label: Int?,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = labelSpacing),
    ) {
        label?.let {
            Text(
                text = stringResource(id = it),
                fontSize = labelTextSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            fontSize = valueTextSize,
            fontWeight = FontWeight.Medium,
        )
        detail?.let {
            Text(
                text = it,
                fontSize = labelTextSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Chip(
    text: String,
    container: Color,
    content: Color,
) {
    Box(
        modifier =
            Modifier
                .heightIn(min = chipHeight)
                .background(
                    color = container,
                    shape = chipShape,
                ).padding(horizontal = chipPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = referenceTextSize,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

@Composable
private fun OverflowMenu(onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = stringResource(id = R.string.more_options),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.delete)) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun EditBar(onEdit: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Button(
            onClick = onEdit,
            shape = buttonShape,
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(all = noteMargin)
                    .fillMaxWidth()
                    .heightIn(min = buttonHeight),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = null,
                modifier = Modifier.size(size = iconSize),
            )
            Text(
                text = stringResource(id = R.string.edit_incident),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = itemSpacing),
            )
        }
    }
}

@Composable
private fun reportedLine(
    incident: Incident,
    names: IncidentNames,
    isMine: Boolean,
): String {
    val reportedAt = incident.reportedAt ?: return stringResource(id = R.string.not_reported_yet)
    val date =
        format(
            instant = reportedAt,
            timeZone = TimeZone.currentSystemDefault(),
            formatter = reportedFormatter,
        )
    return if (isMine) {
        stringResource(id = R.string.reported_by_you, date)
    } else {
        stringResource(
            id = R.string.reported_by,
            names.reporterName ?: stringResource(id = R.string.value_unknown),
            date,
        )
    }
}

@Composable
private fun yesNo(value: Boolean?): String = stringResource(
    id =
        when (value) {
            true -> R.string.value_yes
            false -> R.string.value_no
            null -> R.string.value_not_set
        },
)

private fun format(
    instant: Instant,
    timeZone: TimeZone,
    formatter: DateTimeFormatter,
): String = instant
    .toLocalDateTime(timeZone = timeZone)
    .toJavaLocalDateTime()
    .format(formatter)
