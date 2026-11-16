package com.jonesl7l.carinfg.feature.incident.list

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentListItem
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSort
import com.jonesl7l.carinfg.core.ui.layout.isCompactHeight
import com.jonesl7l.carinfg.core.ui.theme.LocalStatusColors
import com.jonesl7l.carinfg.feature.account.AccountSheet
import com.jonesl7l.carinfg.feature.incident.incidentUndoWindow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private val horizontalPadding = 16.dp
private val sectionHeaderPadding =
    PaddingValues(
        start = 20.dp,
        top = 14.dp,
        end = 20.dp,
        bottom = 6.dp,
    )
private val avatarSize = 40.dp
private val filterSpacing = 8.dp
private val filterShape = RoundedCornerShape(10.dp)
private val fabShape = RoundedCornerShape(16.dp)
private val emptyPadding = 32.dp
private val listBottomPadding = 96.dp
private val sectionHeaderSize = 13.sp
private val avatarTextSize = 14.sp
private val clockTick = 1.minutes
private const val FILTERS_KEY = "filters"
private const val LOADING_KEY = "loading"
private const val EMPTY_KEY = "empty"
private const val NEEDS_ATTENTION_HEADER_KEY = "header-needs-attention"
private const val DRAFTS_HEADER_KEY = "header-drafts"
private const val INCIDENTS_HEADER_KEY = "header-incidents"

@Composable
fun IncidentListScreen(
    pendingUndoId: String?,
    selectedIncidentId: String?,
    onUndoFinish: () -> Unit,
    onOpenIncident: (String) -> Unit,
    onReportIncident: () -> Unit,
    viewModel: IncidentListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    UndoDeleteEffect(
        pendingUndoId = pendingUndoId,
        snackbarHostState = snackbarHostState,
        onUndo = viewModel::undoDelete,
        onSend = viewModel::sendDeleteNow,
        onFinish = onUndoFinish,
    )
    IncidentListContent(
        uiState = uiState,
        selectedIncidentId = selectedIncidentId,
        onScopeChange = viewModel::onScopeChange,
        onStatusFilterChange = viewModel::onStatusFilterChange,
        onSortChange = viewModel::onSortChange,
        onRefresh = viewModel::onRefresh,
        onSyncStatusClick = viewModel::onSyncStatusClick,
        onOpenIncident = onOpenIncident,
        onReportIncident = onReportIncident,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun UndoDeleteEffect(
    pendingUndoId: String?,
    snackbarHostState: SnackbarHostState,
    onUndo: (String) -> Unit,
    onSend: (String) -> Unit,
    onFinish: () -> Unit,
) {
    val message = stringResource(id = R.string.incident_deleted)
    val action = stringResource(id = R.string.undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnSend by rememberUpdatedState(onSend)
    val currentOnFinish by rememberUpdatedState(onFinish)
    LaunchedEffect(pendingUndoId) {
        val incidentId = pendingUndoId ?: return@LaunchedEffect
        var isUndone = false
        try {
            val result =
                withTimeoutOrNull(timeout = incidentUndoWindow) {
                    snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = action,
                        duration = SnackbarDuration.Indefinite,
                    )
                }
            isUndone = result == SnackbarResult.ActionPerformed
            if (isUndone) currentOnUndo(incidentId)
        } finally {
            if (!isUndone) currentOnSend(incidentId)
            currentOnFinish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentListContent(
    uiState: IncidentListUiState,
    selectedIncidentId: String?,
    onScopeChange: (IncidentScope) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onSortChange: (IncidentSort) -> Unit,
    onRefresh: () -> Unit,
    onSyncStatusClick: () -> Unit,
    onOpenIncident: (String) -> Unit,
    onReportIncident: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showAccount by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var shownSort by rememberSaveable { mutableStateOf(uiState.sort) }
    SideEffect {
        if (uiState.sort == shownSort) return@SideEffect
        shownSort = uiState.sort
        listState.requestScrollToItem(index = 0)
    }
    val now by produceState(initialValue = Clock.System.now()) {
        while (true) {
            delay(duration = clockTick)
            value = Clock.System.now()
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.incidents_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    SyncStatusPill(
                        status = uiState.syncStatus,
                        onClick = onSyncStatusClick,
                    )
                    SortMenu(
                        sort = uiState.sort,
                        onSortChange = onSortChange,
                    )
                    Avatar(
                        initials = uiState.userInitials,
                        name = uiState.userName,
                        onClick = { showAccount = true },
                    )
                },
            )
        },
        floatingActionButton = {
            val isCompactHeight = isCompactHeight()
            ExtendedFloatingActionButton(
                text = { Text(text = stringResource(id = R.string.report_incident)) },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = if (isCompactHeight) stringResource(id = R.string.report_incident) else null,
                    )
                },
                onClick = onReportIncident,
                expanded = !isCompactHeight,
                shape = fabShape,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues = innerPadding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = listBottomPadding),
            ) {
                item(key = FILTERS_KEY) {
                    Filters(
                        scope = uiState.scope,
                        statusFilter = uiState.statusFilter,
                        onScopeChange = onScopeChange,
                        onStatusFilterChange = onStatusFilterChange,
                    )
                }
                when {
                    uiState.isLoading -> item(key = LOADING_KEY) { Loading() }

                    uiState.isEmpty -> item(key = EMPTY_KEY) { Empty(scope = uiState.scope) }

                    else -> sections(
                        uiState = uiState,
                        selectedIncidentId = selectedIncidentId,
                        now = now,
                        onOpenIncident = onOpenIncident,
                    )
                }
            }
        }
    }
    if (showAccount) AccountSheet(onDismiss = { showAccount = false })
}

private fun LazyListScope.sections(
    uiState: IncidentListUiState,
    selectedIncidentId: String?,
    now: Instant,
    onOpenIncident: (String) -> Unit,
) {
    val sections = uiState.sections
    if (sections.needsAttention.isNotEmpty()) {
        item(key = NEEDS_ATTENTION_HEADER_KEY) {
            SectionHeader(
                text = stringResource(id = R.string.section_needs_attention, sections.needsAttention.size),
                color = LocalStatusColors.current.onAttention,
                modifier = Modifier.animateItem(),
            )
        }
        rows(
            items = sections.needsAttention,
            selectedIncidentId = selectedIncidentId,
            now = now,
            onOpenIncident = onOpenIncident,
        )
    }
    if (sections.drafts.isNotEmpty()) {
        item(key = DRAFTS_HEADER_KEY) {
            SectionHeader(
                text = stringResource(id = R.string.section_drafts, sections.drafts.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.animateItem(),
            )
        }
        rows(
            items = sections.drafts,
            selectedIncidentId = selectedIncidentId,
            now = now,
            onOpenIncident = onOpenIncident,
        )
    }
    if (sections.incidents.isNotEmpty()) {
        item(key = INCIDENTS_HEADER_KEY) {
            SectionHeader(
                text = stringResource(id = uiState.sort.sectionLabel()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.animateItem(),
            )
        }
        rows(
            items = sections.incidents,
            selectedIncidentId = selectedIncidentId,
            now = now,
            onOpenIncident = onOpenIncident,
        )
    }
}

private fun LazyListScope.rows(
    items: List<IncidentListItem>,
    selectedIncidentId: String?,
    now: Instant,
    onOpenIncident: (String) -> Unit,
) {
    items(
        items = items,
        key = { it.id },
    ) { item ->
        Column(modifier = Modifier.animateItem()) {
            IncidentRow(
                item = item,
                isSelected = item.id == selectedIncidentId,
                now = now,
                onClick = { onOpenIncident(item.id) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun SortMenu(
    sort: IncidentSort,
    onSortChange: (IncidentSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val sortLabel = stringResource(id = sort.label())
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_sort),
                contentDescription = stringResource(id = R.string.sort_description, sortLabel),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Text(
                text = stringResource(id = R.string.sort_by),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        horizontal = horizontalPadding,
                        vertical = filterSpacing,
                    ),
            )
            IncidentSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = option.label())) },
                    onClick = {
                        expanded = false
                        onSortChange(option)
                    },
                    trailingIcon = {
                        if (option == sort) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun Avatar(
    initials: String,
    name: String,
    onClick: () -> Unit,
) {
    val description = stringResource(id = R.string.account_description, name)
    Box(
        modifier =
            Modifier
                .padding(end = filterSpacing)
                .size(size = avatarSize)
                .clip(shape = CircleShape)
                .background(color = MaterialTheme.colorScheme.primary)
                .clickable(onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = avatarTextSize,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Filters(
    scope: IncidentScope,
    statusFilter: StatusFilter,
    onScopeChange: (IncidentScope) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(vertical = filterSpacing),
        verticalArrangement = Arrangement.spacedBy(space = filterSpacing),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
        ) {
            IncidentScope.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == scope,
                    onClick = { onScopeChange(option) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = IncidentScope.entries.size,
                        ),
                ) {
                    Text(text = stringResource(id = option.label()))
                }
            }
        }
        Row(
            modifier =
                Modifier
                    .horizontalScroll(state = rememberScrollState())
                    .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(space = filterSpacing),
        ) {
            StatusFilter.entries.forEach { option ->
                FilterChip(
                    selected = option == statusFilter,
                    onClick = { onStatusFilterChange(option) },
                    label = { Text(text = stringResource(id = option.label())) },
                    shape = filterShape,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = sectionHeaderSize,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(paddingValues = sectionHeaderPadding),
    )
}

@Composable
private fun Loading() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(all = emptyPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun Empty(scope: IncidentScope) {
    Text(
        text =
            stringResource(
                id = if (scope == IncidentScope.MINE) R.string.list_empty_mine else R.string.list_empty_team,
            ),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(all = emptyPadding),
    )
}

@StringRes
private fun IncidentSort.label(): Int = when (this) {
    IncidentSort.NEWEST -> R.string.sort_newest
    IncidentSort.SEVERITY -> R.string.sort_severity
    IncidentSort.A_TO_Z -> R.string.sort_a_to_z
}

@StringRes
private fun IncidentSort.sectionLabel(): Int = when (this) {
    IncidentSort.NEWEST -> R.string.section_all_newest
    IncidentSort.SEVERITY -> R.string.section_all_severity
    IncidentSort.A_TO_Z -> R.string.section_all_a_to_z
}

@StringRes
private fun IncidentScope.label(): Int = when (this) {
    IncidentScope.MINE -> R.string.scope_mine
    IncidentScope.TEAM_OPEN -> R.string.scope_team_open
}

@StringRes
private fun StatusFilter.label(): Int = when (this) {
    StatusFilter.ALL -> R.string.filter_all
    StatusFilter.OPEN -> R.string.status_open
    StatusFilter.ASSIGNED -> R.string.status_assigned
    StatusFilter.IN_PROGRESS -> R.string.status_in_progress
    StatusFilter.RESOLVED -> R.string.status_resolved
}
