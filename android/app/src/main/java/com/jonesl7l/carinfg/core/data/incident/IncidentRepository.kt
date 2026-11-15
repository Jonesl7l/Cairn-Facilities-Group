package com.jonesl7l.carinfg.core.data.incident

import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    fun observeIncident(id: String): Flow<Incident?>

    suspend fun saveDraft(
        incident: Incident,
        now: Instant,
    ): DraftResult

    suspend fun submit(
        incident: Incident,
        now: Instant,
    ): SubmitResult

    suspend fun saveEdit(
        incident: Incident,
        now: Instant,
    ): EditResult

    suspend fun delete(
        id: String,
        now: Instant,
        undoWindow: Duration,
    ): DeleteResult

    suspend fun undoDelete(
        id: String,
        now: Instant,
    ): Boolean

    suspend fun sendDeleteNow(
        id: String,
        now: Instant,
    )
}
