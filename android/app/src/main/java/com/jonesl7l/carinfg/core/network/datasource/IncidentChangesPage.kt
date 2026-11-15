package com.jonesl7l.carinfg.core.network.datasource

import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto

data class IncidentChangesPage(
    val incidents: List<IncidentDto>,
    val databaseId: String,
    val isLastPage: Boolean,
)
