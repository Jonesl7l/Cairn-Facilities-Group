package com.jonesl7l.carinfg.feature.incident.list

import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSort

data class IncidentListUiState(
    val isLoading: Boolean = true,
    val userInitials: String = "",
    val userName: String = "",
    val scope: IncidentScope = IncidentScope.MINE,
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val sort: IncidentSort = IncidentSort.NEWEST,
    val sections: IncidentListSections =
        IncidentListSections(
            needsAttention = emptyList(),
            drafts = emptyList(),
            incidents = emptyList(),
        ),
) {
    val isEmpty: Boolean
        get() = sections.needsAttention.isEmpty() && sections.drafts.isEmpty() && sections.incidents.isEmpty()
}
