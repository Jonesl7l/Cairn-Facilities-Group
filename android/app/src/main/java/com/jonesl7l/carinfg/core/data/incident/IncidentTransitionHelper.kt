package com.jonesl7l.carinfg.core.data.incident

import com.jonesl7l.carinfg.core.data.mapper.WireValueMapper
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import kotlin.time.Duration
import kotlin.time.Instant

object IncidentTransitionHelper {
    fun saveDraft(entity: IncidentEntity): IncidentEntity? {
        if (entity.syncState != SyncState.DRAFT) return null
        return entity.bumped()
    }

    fun submit(
        entity: IncidentEntity,
        now: Instant,
    ): IncidentEntity? {
        if (entity.syncState != SyncState.DRAFT) return null
        return entity
            .copy(
                syncState = SyncState.PENDING_CREATE,
                reportedAt = entity.reportedAt ?: now,
            ).bumped()
    }

    fun edit(entity: IncidentEntity): IncidentEntity? = when (entity.syncState) {
        SyncState.SYNCED -> entity.copy(syncState = SyncState.PENDING_UPDATE).bumped()
        SyncState.PENDING_CREATE, SyncState.PENDING_UPDATE -> entity.bumped()
        SyncState.NEEDS_ATTENTION -> fixValidation(entity = entity)
        SyncState.DRAFT, SyncState.PENDING_DELETE -> null
    }

    fun delete(
        entity: IncidentEntity,
        now: Instant,
        undoWindow: Duration,
    ): DeleteTransition = when (entity.syncState) {
        SyncState.DRAFT -> DeleteTransition.Remove

        SyncState.PENDING_DELETE -> DeleteTransition.NotAllowed

        SyncState.SYNCED,
        SyncState.PENDING_CREATE,
        SyncState.PENDING_UPDATE,
        SyncState.NEEDS_ATTENTION,
        -> DeleteTransition.MarkPending(
            entity =
                entity
                    .copy(
                        syncState = SyncState.PENDING_DELETE,
                        previousSyncState = entity.syncState,
                        undoDeadline = now + undoWindow,
                        deletedAt = now,
                        deletedBy = entity.reporterId,
                    ).bumped(),
        )
    }

    fun undoDelete(entity: IncidentEntity): IncidentEntity? {
        if (entity.syncState != SyncState.PENDING_DELETE) return null
        val previous = entity.previousSyncState ?: return null
        return entity
            .copy(
                syncState = previous,
                previousSyncState = null,
                undoDeadline = null,
                deletedAt = null,
                deletedBy = null,
            ).bumped()
    }

    fun sendDeleteNow(
        entity: IncidentEntity,
        now: Instant,
    ): IncidentEntity? {
        if (entity.syncState != SyncState.PENDING_DELETE) return null
        return entity.copy(undoDeadline = now).bumped()
    }

    // region resolve
    fun canDiscard(entity: IncidentEntity): Boolean = entity.hasConflict(reason = ConflictReason.DELETED_REMOTELY)

    fun canDiscardMyChanges(entity: IncidentEntity): Boolean = entity.hasConflict(reason = ConflictReason.STATUS_LOCKED) && entity.deletedAt == null

    fun submitAsNew(
        entity: IncidentEntity,
        newId: String,
        now: Instant,
    ): IncidentEntity? {
        if (!canDiscard(entity = entity)) return null
        return entity.copy(
            id = newId,
            referenceNumber = null,
            reportedAt = now,
            status = WireValueMapper.toWire(status = IncidentStatus.Open),
            assignedTo = null,
            dueDate = null,
            rootCause = null,
            correctiveActions = null,
            regulatoryReportable = null,
            supervisorAcknowledged = null,
            createdAt = null,
            updatedAt = null,
            serverVersion = null,
            changeSeq = null,
            deletedAt = null,
            deletedBy = null,
            syncState = SyncState.PENDING_CREATE,
            conflictReason = null,
            previousSyncState = null,
            undoDeadline = null,
            localRevision = 1,
            lastSyncError = null,
            syncAttempts = 0,
            localUpdatedAt = now,
        )
    }

    fun keepIncident(entity: IncidentEntity): IncidentEntity? {
        if (!entity.hasConflict(reason = ConflictReason.STATUS_LOCKED) || entity.deletedAt == null) return null
        val previous = entity.previousSyncState
        val restored =
            when (previous) {
                null, SyncState.NEEDS_ATTENTION -> SyncState.PENDING_UPDATE
                else -> previous
            }
        return entity
            .copy(
                syncState = restored,
                previousSyncState = null,
                undoDeadline = null,
                deletedAt = null,
                deletedBy = null,
            ).cleared()
            .bumped()
    }

    private fun fixValidation(entity: IncidentEntity): IncidentEntity? {
        if (!entity.hasConflict(reason = ConflictReason.VALIDATION)) return null
        val retry = if (entity.serverVersion == null) SyncState.PENDING_CREATE else SyncState.PENDING_UPDATE
        return entity
            .copy(syncState = retry)
            .cleared()
            .bumped()
    }

    private fun IncidentEntity.hasConflict(reason: ConflictReason) = syncState == SyncState.NEEDS_ATTENTION && conflictReason == reason

    private fun IncidentEntity.cleared() = copy(
        conflictReason = null,
        lastSyncError = null,
        syncAttempts = 0,
    )
    // endregion

    private fun IncidentEntity.bumped() = copy(localRevision = localRevision + 1)
}
