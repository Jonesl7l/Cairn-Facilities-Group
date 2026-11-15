package com.jonesl7l.carinfg.core.sync.pull

import com.jonesl7l.carinfg.core.data.mapper.toEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.sync.keepingLocalBookkeeping
import com.jonesl7l.carinfg.core.sync.toPhotoEntitiesKeepingPaths
import com.jonesl7l.carinfg.core.sync.withServerFields
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

object PullRuleHelper {
    val scopeWindow = 30.days
    val closedStatuses = setOf("RESOLVED", "CLOSED")
    private const val OPEN_STATUS = "OPEN"

    // region decide

    fun decide(
        local: IncidentWithPhotos?,
        remote: IncidentDto,
        userId: String,
        now: Instant,
    ): PullAction {
        if (local == null) {
            return decideForNew(
                remote = remote,
                userId = userId,
                now = now,
            )
        }
        val row = local.incident
        return when (row.syncState) {
            SyncState.DRAFT -> PullAction.Skip

            SyncState.SYNCED -> decideForSynced(
                local = local,
                remote = remote,
                userId = userId,
                now = now,
            )

            SyncState.PENDING_CREATE -> decideForPendingCreate(
                row = row,
                remote = remote,
            )

            SyncState.PENDING_UPDATE,
            SyncState.NEEDS_ATTENTION,
            -> decideForLocalEdits(
                row = row,
                remote = remote,
            )

            SyncState.PENDING_DELETE -> decideForPendingDelete(
                row = row,
                remote = remote,
            )
        }
    }

    private fun decideForNew(
        remote: IncidentDto,
        userId: String,
        now: Instant,
    ): PullAction = if (remote.isTombstone || !isInScope(remote = remote, userId = userId, now = now)) {
        PullAction.Skip
    } else {
        overwrite(remote = remote, local = null)
    }

    private fun decideForSynced(
        local: IncidentWithPhotos,
        remote: IncidentDto,
        userId: String,
        now: Instant,
    ): PullAction = if (remote.isTombstone || !isInScope(remote = remote, userId = userId, now = now)) {
        PullAction.Delete(id = local.incident.id, basedOnRevision = local.incident.localRevision)
    } else {
        overwrite(remote = remote, local = local)
    }

    private fun decideForPendingCreate(
        row: IncidentEntity,
        remote: IncidentDto,
    ): PullAction = if (remote.isTombstone) {
        needsAttention(row = row, remote = remote, reason = ConflictReason.DELETED_REMOTELY)
    } else {
        keep(row = row, entity = remote.toEntity().keepingLocalBookkeeping(row = row))
    }

    private fun decideForLocalEdits(
        row: IncidentEntity,
        remote: IncidentDto,
    ): PullAction = if (remote.isTombstone) {
        needsAttention(row = row, remote = remote, reason = ConflictReason.DELETED_REMOTELY)
    } else {
        keep(row = row, entity = row.withServerFields(remote = remote))
    }

    private fun decideForPendingDelete(
        row: IncidentEntity,
        remote: IncidentDto,
    ): PullAction = when {
        // the user wanted it gone, so a remote delete needs no attention
        remote.isTombstone -> PullAction.Delete(id = row.id, basedOnRevision = row.localRevision)

        remote.status != OPEN_STATUS -> {
            needsAttention(row = row, remote = remote, reason = ConflictReason.STATUS_LOCKED)
        }

        else -> keep(row = row, entity = row.withServerFields(remote = remote))
    }

    // endregion

    fun isInScope(
        remote: IncidentDto,
        userId: String,
        now: Instant,
    ): Boolean = remote.reporterId == userId ||
        remote.status !in closedStatuses ||
        remote.updatedAt >= now - scopeWindow

    private fun overwrite(
        remote: IncidentDto,
        local: IncidentWithPhotos?,
    ): PullAction = PullAction.Upsert(
        entity = remote.toEntity(),
        replacementPhotos = remote.toPhotoEntitiesKeepingPaths(localPhotos = local?.photos.orEmpty()),
        basedOnRevision = local?.incident?.localRevision,
    )

    private fun keep(
        row: IncidentEntity,
        entity: IncidentEntity,
    ): PullAction = PullAction.Upsert(
        entity = entity,
        replacementPhotos = null,
        basedOnRevision = row.localRevision,
    )

    private fun needsAttention(
        row: IncidentEntity,
        remote: IncidentDto,
        reason: ConflictReason,
    ): PullAction = keep(
        row = row,
        entity =
            row.withServerFields(remote = remote).copy(
                syncState = SyncState.NEEDS_ATTENTION,
                conflictReason = reason,
            ),
    )

    private val IncidentDto.isTombstone: Boolean
        get() = deletedAt != null
}
