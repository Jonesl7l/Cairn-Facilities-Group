package com.jonesl7l.carinfg.feature.incident.list

import com.jonesl7l.carinfg.core.domain.model.incident.IncidentListItem
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSort
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState

object IncidentListHelper {
    private val closedStatuses = setOf(IncidentStatus.Resolved, IncidentStatus.Closed)

    private val newestFirst = compareByDescending<IncidentListItem> { it.occurredAt }

    private const val STILL_TRYING_ATTEMPTS = 8

    fun sections(
        items: List<IncidentListItem>,
        userId: String,
        scope: IncidentScope,
        statusFilter: StatusFilter,
        sort: IncidentSort,
    ): IncidentListSections {
        val inScope =
            items.filter {
                it.isInScope(
                    userId = userId,
                    scope = scope,
                )
            }
        val (pinned, rest) = inScope.partition {
            it.syncState == SyncState.NEEDS_ATTENTION ||
                it.syncState == SyncState.DRAFT
        }
        return IncidentListSections(
            needsAttention = pinned.filter { it.syncState == SyncState.NEEDS_ATTENTION }.sortedWith(newestFirst),
            drafts = pinned.filter { it.syncState == SyncState.DRAFT }.sortedWith(newestFirst),
            incidents = rest.filter { it.matches(statusFilter = statusFilter) }.sortedWith(sort.comparator()),
        )
    }

    fun syncChip(item: IncidentListItem): SyncChip? = when (item.syncState) {
        SyncState.SYNCED -> null

        SyncState.DRAFT -> SyncChip.DRAFT

        SyncState.NEEDS_ATTENTION -> SyncChip.NEEDS_ATTENTION

        SyncState.PENDING_CREATE,
        SyncState.PENDING_UPDATE,
        SyncState.PENDING_DELETE,
        -> if (item.syncAttempts >= STILL_TRYING_ATTEMPTS) SyncChip.STILL_TRYING else SyncChip.PENDING
    }

    private fun IncidentListItem.isInScope(
        userId: String,
        scope: IncidentScope,
    ): Boolean = when (scope) {
        IncidentScope.MINE -> reporterId == userId
        IncidentScope.TEAM_OPEN -> reporterId != userId && status !in closedStatuses
    }

    private fun IncidentListItem.matches(statusFilter: StatusFilter): Boolean = when (statusFilter) {
        StatusFilter.ALL -> true
        StatusFilter.OPEN -> status == IncidentStatus.Open
        StatusFilter.ASSIGNED -> status == IncidentStatus.Assigned
        StatusFilter.IN_PROGRESS -> status == IncidentStatus.InProgress
        StatusFilter.RESOLVED -> status in closedStatuses
    }

    private fun IncidentSort.comparator(): Comparator<IncidentListItem> = when (this) {
        IncidentSort.NEWEST -> newestFirst
        IncidentSort.SEVERITY -> compareByDescending<IncidentListItem> { it.severity?.rank ?: 0 }.then(newestFirst)
        IncidentSort.A_TO_Z -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    }
}
