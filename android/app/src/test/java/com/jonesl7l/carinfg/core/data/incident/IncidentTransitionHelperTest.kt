package com.jonesl7l.carinfg.core.data.incident

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.core.database.EntityTestHelper.draftIncidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.now
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.Test

class IncidentTransitionHelperTest {
    private val undoWindow = 5.seconds

    // region saveDraft
    @Test
    fun `saving a draft keeps it a draft and bumps the revision`() {
        val draft = draftIncidentEntity()

        assertThat(IncidentTransitionHelper.saveDraft(entity = draft)).isEqualTo(draft.copy(localRevision = 1))
    }

    @Test
    fun `only drafts can be saved as drafts`() {
        assertThat(IncidentTransitionHelper.saveDraft(entity = incidentEntity(syncState = SyncState.SYNCED))).isNull()
    }
    // endregion

    // region submit
    @Test
    fun `submitting a draft queues its create and stamps reportedAt`() {
        val draft = draftIncidentEntity()

        val submitted = IncidentTransitionHelper.submit(
            entity = draft,
            now = now,
        )

        val expected =
            draft.copy(
                syncState = SyncState.PENDING_CREATE,
                reportedAt = now,
                localRevision = 1,
            )
        assertThat(submitted).isEqualTo(expected)
    }

    @Test
    fun `submitting keeps a reportedAt that is already set`() {
        val reportedAt = now - 1.hours
        val draft = draftIncidentEntity().copy(reportedAt = reportedAt)

        assertThat(
            IncidentTransitionHelper.submit(
                entity = draft,
                now = now,
            )?.reportedAt,
        ).isEqualTo(reportedAt)
    }

    @Test
    fun `only drafts can be submitted`() {
        val synced = incidentEntity(syncState = SyncState.SYNCED)

        assertThat(
            IncidentTransitionHelper.submit(
                entity = synced,
                now = now,
            ),
        ).isNull()
    }
    // endregion

    // region edit
    @Test
    fun `editing a synced incident queues an update`() {
        val edited = IncidentTransitionHelper.edit(entity = incidentEntity(syncState = SyncState.SYNCED))

        assertThat(edited?.syncState).isEqualTo(SyncState.PENDING_UPDATE)
        assertThat(edited?.localRevision).isEqualTo(1L)
    }

    @Test
    fun `editing a pending incident keeps its state so the latest version is sent`() {
        listOf(SyncState.PENDING_CREATE, SyncState.PENDING_UPDATE).forEach { state ->
            val edited = IncidentTransitionHelper.edit(entity = incidentEntity(syncState = state))

            assertThat(edited?.syncState).isEqualTo(state)
            assertThat(edited?.localRevision).isEqualTo(1L)
        }
    }

    @Test
    fun `drafts, pending deletes and incidents needing attention cannot be edited`() {
        listOf(SyncState.DRAFT, SyncState.PENDING_DELETE, SyncState.NEEDS_ATTENTION).forEach { state ->
            assertThat(IncidentTransitionHelper.edit(entity = incidentEntity(syncState = state))).isNull()
        }
    }

    @Test
    fun `fixing a validation problem sends the incident again`() {
        val updated = IncidentTransitionHelper.edit(entity = conflict(reason = ConflictReason.VALIDATION))
        val created = IncidentTransitionHelper.edit(
            entity = conflict(
                reason = ConflictReason.VALIDATION,
                serverVersion = null,
            ),
        )

        assertThat(updated?.syncState).isEqualTo(SyncState.PENDING_UPDATE)
        assertThat(updated?.conflictReason).isNull()
        assertThat(created?.syncState).isEqualTo(SyncState.PENDING_CREATE)
    }

    @Test
    fun `other conflicts cannot be edited`() {
        assertThat(IncidentTransitionHelper.edit(entity = conflict(reason = ConflictReason.STATUS_LOCKED))).isNull()
    }
    // endregion

    // region resolve
    @Test
    fun `submitting as new copies the report under a new id with no server fields`() {
        val copy = IncidentTransitionHelper.submitAsNew(
            entity = conflict(reason = ConflictReason.DELETED_REMOTELY),
            newId = "incident-2",
            now = now,
        )

        assertThat(copy?.id).isEqualTo("incident-2")
        assertThat(copy?.title).isEqualTo(incidentEntity().title)
        assertThat(copy?.referenceNumber).isNull()
        assertThat(copy?.serverVersion).isNull()
        assertThat(copy?.status).isEqualTo("OPEN")
        assertThat(copy?.syncState).isEqualTo(SyncState.PENDING_CREATE)
        assertThat(copy?.conflictReason).isNull()
    }

    @Test
    fun `only an incident deleted remotely can be discarded or submitted as new`() {
        val locked = conflict(reason = ConflictReason.STATUS_LOCKED)

        assertThat(IncidentTransitionHelper.canDiscard(entity = locked)).isFalse()
        assertThat(
            IncidentTransitionHelper.submitAsNew(
                entity = locked,
                newId = "incident-2",
                now = now,
            ),
        ).isNull()
    }

    @Test
    fun `keeping a locked delete restores its previous state`() {
        val kept = IncidentTransitionHelper.keepIncident(
            entity = conflict(
                reason = ConflictReason.STATUS_LOCKED,
                deletedAt = now,
            ),
        )

        assertThat(kept?.syncState).isEqualTo(SyncState.SYNCED)
        assertThat(kept?.deletedAt).isNull()
        assertThat(kept?.conflictReason).isNull()
    }

    @Test
    fun `a locked edit can discard its changes but not be kept`() {
        val lockedEdit = conflict(reason = ConflictReason.STATUS_LOCKED)

        assertThat(IncidentTransitionHelper.canDiscardMyChanges(entity = lockedEdit)).isTrue()
        assertThat(IncidentTransitionHelper.keepIncident(entity = lockedEdit)).isNull()
    }
    // endregion

    // region delete
    @Test
    fun `deleting a draft removes it`() {
        val transition =
            IncidentTransitionHelper.delete(
                entity = draftIncidentEntity(),
                now = now,
                undoWindow = undoWindow,
            )

        assertThat(transition).isEqualTo(DeleteTransition.Remove)
    }

    @Test
    fun `deleting an incident that may have reached the server queues the delete with undo`() {
        val states =
            listOf(
                SyncState.SYNCED,
                SyncState.PENDING_CREATE,
                SyncState.PENDING_UPDATE,
                SyncState.NEEDS_ATTENTION,
            )

        states.forEach { state ->
            val entity = incidentEntity(syncState = state)

            val transition =
                IncidentTransitionHelper.delete(
                    entity = entity,
                    now = now,
                    undoWindow = undoWindow,
                )

            val expected =
                entity.copy(
                    syncState = SyncState.PENDING_DELETE,
                    previousSyncState = state,
                    undoDeadline = now + undoWindow,
                    deletedAt = now,
                    deletedBy = entity.reporterId,
                    localRevision = 1,
                )
            assertThat(transition).isEqualTo(DeleteTransition.MarkPending(entity = expected))
        }
    }

    @Test
    fun `an incident already being deleted cannot be deleted again`() {
        val transition =
            IncidentTransitionHelper.delete(
                entity = incidentEntity(syncState = SyncState.PENDING_DELETE),
                now = now,
                undoWindow = undoWindow,
            )

        assertThat(transition).isEqualTo(DeleteTransition.NotAllowed)
    }
    // endregion

    // region undoDelete
    @Test
    fun `undo restores the previous state and clears the delete`() {
        val original = incidentEntity(syncState = SyncState.PENDING_UPDATE)
        val transition =
            IncidentTransitionHelper.delete(
                entity = original,
                now = now,
                undoWindow = undoWindow,
            )
        val pending = (transition as DeleteTransition.MarkPending).entity

        assertThat(IncidentTransitionHelper.undoDelete(entity = pending)).isEqualTo(original.copy(localRevision = 2))
    }

    @Test
    fun `undo only applies to a pending delete`() {
        assertThat(IncidentTransitionHelper.undoDelete(entity = incidentEntity(syncState = SyncState.SYNCED))).isNull()
    }
    // endregion

    // region sendDeleteNow
    @Test
    fun `dismissing the undo sends the delete now`() {
        val pending =
            incidentEntity(
                syncState = SyncState.PENDING_DELETE,
                undoDeadline = now + undoWindow,
            )

        val sent = IncidentTransitionHelper.sendDeleteNow(
            entity = pending,
            now = now,
        )

        assertThat(sent?.undoDeadline).isEqualTo(now)
    }

    @Test
    fun `send now only applies to a pending delete`() {
        val synced = incidentEntity(syncState = SyncState.SYNCED)

        assertThat(
            IncidentTransitionHelper.sendDeleteNow(
                entity = synced,
                now = now,
            ),
        ).isNull()
    }
    // endregion

    private fun conflict(
        reason: ConflictReason,
        serverVersion: Int? = 1,
        deletedAt: Instant? = null,
    ) = incidentEntity(
        serverVersion = serverVersion,
        syncState = SyncState.NEEDS_ATTENTION,
        conflictReason = reason,
        previousSyncState = deletedAt?.let { SyncState.SYNCED },
        deletedAt = deletedAt,
        deletedBy = deletedAt?.let { "u1" },
    )
}
