package com.jonesl7l.carinfg.core.data.preferences

import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSort
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserPreferencesStore : UserPreferencesStore {
    override val incidentSort = MutableStateFlow(IncidentSort.NEWEST)

    override suspend fun setIncidentSort(sort: IncidentSort) {
        incidentSort.value = sort
    }
}
