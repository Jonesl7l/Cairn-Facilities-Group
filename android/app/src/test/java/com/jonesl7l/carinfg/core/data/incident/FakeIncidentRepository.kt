package com.jonesl7l.carinfg.core.data.incident

import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentDetail
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentListItem
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeIncidentRepository : IncidentRepository {
    val incidentList = MutableStateFlow<List<IncidentListItem>>(emptyList())
    val incidentDetail = MutableStateFlow<IncidentDetail?>(null)
    var deleteResult: DeleteResult = DeleteResult.NotAllowed
    val deletedIds = mutableListOf<String>()
    val undoneIds = mutableListOf<String>()
    val sentNowIds = mutableListOf<String>()

    override fun observeIncident(id: String): Flow<Incident?> = unused()

    override fun observeIncidentList(): Flow<List<IncidentListItem>> = incidentList

    override fun observeIncidentDetail(id: String): Flow<IncidentDetail?> = incidentDetail

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
    ): DeleteResult {
        deletedIds += id
        return deleteResult
    }

    override suspend fun undoDelete(
        id: String,
        now: Instant,
    ): Boolean {
        undoneIds += id
        return true
    }

    override suspend fun sendDeleteNow(
        id: String,
        now: Instant,
    ) {
        sentNowIds += id
    }

    private fun unused(): Nothing = throw UnsupportedOperationException("not used by these tests")
}
