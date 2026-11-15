package com.jonesl7l.carinfg.core.data.incident

import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentListItem
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeIncidentRepository : IncidentRepository {
    val incidentList = MutableStateFlow<List<IncidentListItem>>(emptyList())

    override fun observeIncident(id: String): Flow<Incident?> = unused()

    override fun observeIncidentList(): Flow<List<IncidentListItem>> = incidentList

    override suspend fun saveDraft(
        incident: Incident,
        now: Instant,
    ): SaveResult = unused()

    override suspend fun submit(
        incident: Incident,
        now: Instant,
    ): SaveResult = unused()

    override suspend fun saveEdit(
        incident: Incident,
        now: Instant,
    ): SaveResult = unused()

    override suspend fun delete(
        id: String,
        now: Instant,
        undoWindow: Duration,
    ): DeleteResult = unused()

    override suspend fun undoDelete(
        id: String,
        now: Instant,
    ): Boolean = unused()

    override suspend fun sendDeleteNow(
        id: String,
        now: Instant,
    ): Unit = unused()

    private fun unused(): Nothing = throw UnsupportedOperationException("not used by these tests")
}
