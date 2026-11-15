package com.jonesl7l.carinfg.core.data.preferences

import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSort
import kotlinx.coroutines.flow.Flow

interface UserPreferencesStore {
    val incidentSort: Flow<IncidentSort>

    suspend fun setIncidentSort(sort: IncidentSort)
}
