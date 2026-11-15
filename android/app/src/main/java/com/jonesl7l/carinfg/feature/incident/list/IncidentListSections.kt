package com.jonesl7l.carinfg.feature.incident.list

import com.jonesl7l.carinfg.core.domain.model.incident.IncidentListItem

data class IncidentListSections(
    val needsAttention: List<IncidentListItem>,
    val drafts: List<IncidentListItem>,
    val incidents: List<IncidentListItem>,
)
