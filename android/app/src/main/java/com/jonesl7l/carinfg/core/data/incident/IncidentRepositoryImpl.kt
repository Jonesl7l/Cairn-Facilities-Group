package com.jonesl7l.carinfg.core.data.incident

import androidx.room3.withWriteTransaction
import com.jonesl7l.carinfg.core.data.mapper.toDomain
import com.jonesl7l.carinfg.core.data.mapper.toEntity
import com.jonesl7l.carinfg.core.data.session.SessionStore
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.dao.IncidentDao
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.policy.IncidentPolicyHelper
import com.jonesl7l.carinfg.core.domain.validation.IncidentValidator
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class IncidentRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val incidentDao: IncidentDao,
    private val sessionStore: SessionStore,
) : IncidentRepository {
    override fun observeIncident(id: String): Flow<Incident?> = incidentDao.observeIncident(id = id)
        .map { it?.toDomain() }
        .distinctUntilChanged()

    override suspend fun saveDraft(
        incident: Incident,
        now: Instant,
    ): DraftResult = database.withWriteTransaction {
        val stored = incidentDao.getIncident(id = incident.id)?.incident
        val draft =
            IncidentTransitionHelper.saveDraft(entity = incident.mergeOnto(stored = stored, now = now))
                ?: return@withWriteTransaction DraftResult.NotADraft
        incidentDao.upsertIncident(incident = draft)
        DraftResult.Saved
    }

    override suspend fun submit(
        incident: Incident,
        now: Instant,
    ): SubmitResult {
        val errors = IncidentValidator.validate(incident = incident, now = now)
        if (errors.isNotEmpty()) return SubmitResult.Invalid(errors = errors)
        return database.withWriteTransaction {
            val stored = incidentDao.getIncident(id = incident.id)?.incident
            val submitted =
                IncidentTransitionHelper.submit(
                    entity = incident.mergeOnto(stored = stored, now = now),
                    now = now,
                ) ?: return@withWriteTransaction SubmitResult.NotADraft
            incidentDao.upsertIncident(incident = submitted)
            SubmitResult.Submitted
        }
    }

    override suspend fun saveEdit(
        incident: Incident,
        now: Instant,
    ): EditResult {
        val userId = currentUserId() ?: return EditResult.NotAllowed
        val errors = IncidentValidator.validate(incident = incident, now = now)
        if (errors.isNotEmpty()) return EditResult.Invalid(errors = errors)
        return database.withWriteTransaction {
            val stored = incidentDao.getIncident(id = incident.id) ?: return@withWriteTransaction EditResult.NotAllowed
            if (!IncidentPolicyHelper.canEdit(incident = stored.toDomain(), userId = userId)) {
                return@withWriteTransaction EditResult.NotAllowed
            }
            val edited =
                IncidentTransitionHelper.edit(entity = incident.mergeOnto(stored = stored.incident, now = now))
                    ?: return@withWriteTransaction EditResult.NotAllowed
            incidentDao.upsertIncident(incident = edited)
            EditResult.Saved
        }
    }

    override suspend fun delete(
        id: String,
        now: Instant,
        undoWindow: Duration,
    ): DeleteResult {
        val userId = currentUserId() ?: return DeleteResult.NotAllowed
        return database.withWriteTransaction {
            val stored = incidentDao.getIncident(id = id) ?: return@withWriteTransaction DeleteResult.NotAllowed
            if (!IncidentPolicyHelper.canDelete(incident = stored.toDomain(), userId = userId)) {
                return@withWriteTransaction DeleteResult.NotAllowed
            }
            val transition =
                IncidentTransitionHelper.delete(
                    entity = stored.incident.copy(localUpdatedAt = now),
                    now = now,
                    undoWindow = undoWindow,
                )
            when (transition) {
                DeleteTransition.Remove -> {
                    incidentDao.deleteIncident(id = id)
                    DeleteResult.Removed
                }

                is DeleteTransition.MarkPending -> {
                    incidentDao.upsertIncident(incident = transition.entity)
                    DeleteResult.PendingDelete(undoDeadline = now + undoWindow)
                }

                DeleteTransition.NotAllowed -> DeleteResult.NotAllowed
            }
        }
    }

    override suspend fun undoDelete(
        id: String,
        now: Instant,
    ): Boolean = database.withWriteTransaction {
        val stored = incidentDao.getIncident(id = id)?.incident ?: return@withWriteTransaction false
        val restored =
            IncidentTransitionHelper.undoDelete(entity = stored.copy(localUpdatedAt = now))
                ?: return@withWriteTransaction false
        incidentDao.upsertIncident(incident = restored)
        true
    }

    override suspend fun sendDeleteNow(
        id: String,
        now: Instant,
    ) {
        database.withWriteTransaction {
            val stored = incidentDao.getIncident(id = id)?.incident ?: return@withWriteTransaction
            val sent = IncidentTransitionHelper.sendDeleteNow(entity = stored, now = now) ?: return@withWriteTransaction
            incidentDao.upsertIncident(incident = sent)
        }
    }

    private suspend fun currentUserId(): String? = sessionStore.session.first()?.id

    private fun Incident.mergeOnto(
        stored: IncidentEntity?,
        now: Instant,
    ): IncidentEntity {
        val fromForm = toEntity(existing = stored, localUpdatedAt = now)
        if (stored == null) return fromForm.copy(syncState = SyncState.DRAFT)
        return fromForm.copy(
            referenceNumber = stored.referenceNumber,
            status = stored.status,
            assignedTo = stored.assignedTo,
            dueDate = stored.dueDate,
            rootCause = stored.rootCause,
            correctiveActions = stored.correctiveActions,
            regulatoryReportable = stored.regulatoryReportable,
            supervisorAcknowledged = stored.supervisorAcknowledged,
            createdAt = stored.createdAt,
            updatedAt = stored.updatedAt,
            deletedAt = stored.deletedAt,
            deletedBy = stored.deletedBy,
            syncState = stored.syncState,
            conflictReason = stored.conflictReason,
            lastSyncError = stored.lastSyncError,
            syncAttempts = stored.syncAttempts,
        )
    }
}
