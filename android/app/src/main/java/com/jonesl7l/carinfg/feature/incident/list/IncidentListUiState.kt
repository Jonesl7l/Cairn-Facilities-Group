package com.jonesl7l.carinfg.feature.incident.list

import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSort
import com.jonesl7l.carinfg.core.sync.status.AppSyncStatus
import com.jonesl7l.carinfg.core.sync.status.SyncIndicator

data class IncidentListUiState(
    val isLoading: Boolean = true,
    val userInitials: String = "",
    val userName: String = "",
    val scope: IncidentScope = IncidentScope.MINE,
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val sort: IncidentSort = IncidentSort.NEWEST,
    val syncStatus: AppSyncStatus = AppSyncStatus(),
    val sections: IncidentListSections =
        IncidentListSections(
            needsAttention = emptyList(),
            drafts = emptyList(),
            incidents = emptyList(),
        ),
) {
    val isRefreshing: Boolean
        get() = syncStatus.indicator == SyncIndicator.Syncing

    val isEmpty: Boolean
        get() = sections.needsAttention.isEmpty() && sections.drafts.isEmpty() && sections.incidents.isEmpty()
}
