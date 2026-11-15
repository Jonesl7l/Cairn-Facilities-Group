package com.jonesl7l.carinfg.core.data.incident

import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
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

    fun edit(entity: IncidentEntity): IncidentEntity? {
        val nextState =
            when (entity.syncState) {
                SyncState.SYNCED -> SyncState.PENDING_UPDATE
                SyncState.PENDING_CREATE, SyncState.PENDING_UPDATE -> entity.syncState
                SyncState.DRAFT, SyncState.PENDING_DELETE, SyncState.NEEDS_ATTENTION -> return null
            }
        return entity.copy(syncState = nextState).bumped()
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

    private fun IncidentEntity.bumped() = copy(localRevision = localRevision + 1)
}
