package com.jonesl7l.carinfg.core.sync.pull

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.core.data.mapper.toEntity
import com.jonesl7l.carinfg.core.data.mapper.toPhotoEntities
import com.jonesl7l.carinfg.core.database.EntityTestHelper.draftIncidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.photoEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.network.ApiTestHelper.REPORTER_ID
import com.jonesl7l.carinfg.core.network.ApiTestHelper.incidentDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.now
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import kotlin.time.Duration.Companion.days
import org.junit.Test

class PullRuleHelperTest {
    private val remote = incidentDto(
        title = "Server title",
        status = "ASSIGNED",
        version = 4,
        changeSeq = 40,
    )
    private val tombstone = incidentDto(
        deletedAt = now,
        deletedBy = "operator1",
        version = 5,
        changeSeq = 41,
    )

    // region no local row or synced
    @Test
    fun `a new live item is stored as synced with its photos`() {
        val action = decide(
            local = null,
            remote = remote,
        )

        val expected =
            PullAction.Upsert(
                entity = remote.toEntity(),
                replacementPhotos = remote.toPhotoEntities(),
                basedOnRevision = null,
            )
        assertThat(action).isEqualTo(expected)
    }

    @Test
    fun `a new tombstone is skipped`() {
        assertThat(
            decide(
                local = null,
                remote = tombstone,
            ),
        ).isEqualTo(PullAction.Skip)
    }

    @Test
    fun `a synced row is overwritten and keeps the local paths of its photos`() {
        val local =
            withPhotos(
                row = incidentEntity(
                    syncState = SyncState.SYNCED,
                    localRevision = 2,
                ),
                photos = listOf(photoEntity(localPath = "/files/photo-0.jpg")),
            )

        val action = decide(
            local = local,
            remote = remote,
        ) as PullAction.Upsert

        assertThat(action.entity).isEqualTo(remote.toEntity())
        assertThat(action.replacementPhotos?.single()?.localPath).isEqualTo("/files/photo-0.jpg")
        assertThat(action.basedOnRevision).isEqualTo(2L)
    }

    @Test
    fun `a synced row with a tombstone is deleted locally`() {
        val local = withPhotos(
            row = incidentEntity(
                syncState = SyncState.SYNCED,
                localRevision = 2,
            ),
        )

        val action = decide(
            local = local,
            remote = tombstone,
        )

        assertThat(action).isEqualTo(
            PullAction.Delete(
                id = local.incident.id,
                basedOnRevision = 2,
            ),
        )
    }
    // endregion

    // region pending
    @Test
    fun `a pending update takes the server fields and keeps the local technician fields`() {
        val row =
            incidentEntity(
                syncState = SyncState.PENDING_UPDATE,
                title = "Local title",
                localRevision = 3,
            )

        val action = decide(
            local = withPhotos(row = row),
            remote = remote,
        ) as PullAction.Upsert

        assertThat(action.entity.title).isEqualTo("Local title")
        assertThat(action.entity.status).isEqualTo("ASSIGNED")
        assertThat(action.entity.serverVersion).isEqualTo(4)
        assertThat(action.entity.syncState).isEqualTo(SyncState.PENDING_UPDATE)
        assertThat(action.replacementPhotos).isEqualTo(null)
        assertThat(action.basedOnRevision).isEqualTo(3L)
    }

    @Test
    fun `any pending row with a tombstone needs attention as deleted remotely`() {
        listOf(SyncState.PENDING_CREATE, SyncState.PENDING_UPDATE).forEach { state ->
            val action = decide(
                local = withPhotos(row = incidentEntity(syncState = state)),
                remote = tombstone,
            )

            val entity = (action as PullAction.Upsert).entity
            assertThat(entity.syncState).isEqualTo(SyncState.NEEDS_ATTENTION)
            assertThat(entity.conflictReason).isEqualTo(ConflictReason.DELETED_REMOTELY)
            assertThat(entity.deletedBy).isEqualTo("operator1")
        }
    }

    @Test
    fun `a pending create that comes back is marked synced`() {
        val row = incidentEntity(
            syncState = SyncState.PENDING_CREATE,
            localRevision = 1,
        )

        val action = decide(
            local = withPhotos(row = row),
            remote = remote,
        ) as PullAction.Upsert

        val expected =
            remote.toEntity().copy(
                localRevision = 1,
                localUpdatedAt = row.localUpdatedAt,
            )
        assertThat(action.entity).isEqualTo(expected)
        assertThat(action.entity.syncState).isEqualTo(SyncState.SYNCED)
    }

    @Test
    fun `a pending delete takes the new version and keeps its delete while still open`() {
        val row = incidentEntity(
            syncState = SyncState.PENDING_DELETE,
            deletedAt = now,
            deletedBy = REPORTER_ID,
        )

        val action = decide(
            local = withPhotos(row = row),
            remote = incidentDto(
                status = "OPEN",
                version = 6,
            ),
        )

        val entity = (action as PullAction.Upsert).entity
        assertThat(entity.syncState).isEqualTo(SyncState.PENDING_DELETE)
        assertThat(entity.serverVersion).isEqualTo(6)
        assertThat(entity.deletedAt).isEqualTo(now)
        assertThat(entity.deletedBy).isEqualTo(REPORTER_ID)
    }

    @Test
    fun `a pending delete needs attention as locked once the status moves on`() {
        val row = incidentEntity(syncState = SyncState.PENDING_DELETE)

        val entity = (
            decide(
                local = withPhotos(row = row),
                remote = remote,
            ) as PullAction.Upsert
            ).entity

        assertThat(entity.syncState).isEqualTo(SyncState.NEEDS_ATTENTION)
        assertThat(entity.conflictReason).isEqualTo(ConflictReason.STATUS_LOCKED)
    }

    @Test
    fun `a pending delete whose incident was deleted remotely is removed locally`() {
        val row = incidentEntity(
            syncState = SyncState.PENDING_DELETE,
            localRevision = 2,
        )

        val action = decide(
            local = withPhotos(row = row),
            remote = tombstone,
        )

        assertThat(action).isEqualTo(
            PullAction.Delete(
                id = row.id,
                basedOnRevision = 2,
            ),
        )
    }
    // endregion

    // region needs attention and drafts
    @Test
    fun `an incident needing attention refreshes its server copy and keeps its state`() {
        val row =
            incidentEntity(
                syncState = SyncState.NEEDS_ATTENTION,
                conflictReason = ConflictReason.VALIDATION,
                title = "Local title",
            )

        val entity = (
            decide(
                local = withPhotos(row = row),
                remote = remote,
            ) as PullAction.Upsert
            ).entity

        assertThat(entity.title).isEqualTo("Local title")
        assertThat(entity.serverVersion).isEqualTo(4)
        assertThat(entity.syncState).isEqualTo(SyncState.NEEDS_ATTENTION)
        assertThat(entity.conflictReason).isEqualTo(ConflictReason.VALIDATION)
    }

    @Test
    fun `drafts are left alone`() {
        assertThat(
            decide(
                local = withPhotos(row = draftIncidentEntity()),
                remote = remote,
            ),
        ).isEqualTo(PullAction.Skip)
    }
    // endregion

    // region scope
    @Test
    fun `scope covers my incidents, others unresolved ones and others changed in the last 30 days`() {
        val old = now - 31.days
        val mine = incidentDto(
            reporterId = REPORTER_ID,
            status = "CLOSED",
            updatedAt = old,
        )
        val othersOpen = incidentDto(
            reporterId = "tech2",
            status = "IN_PROGRESS",
            updatedAt = old,
        )
        val othersRecent = incidentDto(
            reporterId = "tech2",
            status = "RESOLVED",
            updatedAt = now - 29.days,
        )
        val othersOld = incidentDto(
            reporterId = "tech2",
            status = "CLOSED",
            updatedAt = old,
        )

        assertThat(inScope(remote = mine)).isTrue()
        assertThat(inScope(remote = othersOpen)).isTrue()
        assertThat(inScope(remote = othersRecent)).isTrue()
        assertThat(inScope(remote = othersOld)).isFalse()
    }

    @Test
    fun `an out of scope item is skipped when new and removed when synced locally`() {
        val othersOld = incidentDto(
            reporterId = "tech2",
            status = "CLOSED",
            updatedAt = now - 31.days,
        )
        val local = withPhotos(
            row = incidentEntity(
                reporterId = "tech2",
                syncState = SyncState.SYNCED,
            ),
        )

        assertThat(
            decide(
                local = null,
                remote = othersOld,
            ),
        ).isEqualTo(PullAction.Skip)
        assertThat(
            decide(
                local = local,
                remote = othersOld,
            ),
        ).isInstanceOf<PullAction.Delete>()
    }
    // endregion

    private fun decide(
        local: IncidentWithPhotos?,
        remote: IncidentDto,
    ): PullAction = PullRuleHelper.decide(
        local = local,
        remote = remote,
        userId = REPORTER_ID,
        now = now,
    )

    private fun inScope(remote: IncidentDto): Boolean = PullRuleHelper.isInScope(
        remote = remote,
        userId = REPORTER_ID,
        now = now,
    )

    private fun withPhotos(
        row: IncidentEntity,
        photos: List<PhotoEntity> = emptyList(),
    ) = IncidentWithPhotos(
        incident = row,
        photos = photos,
    )
}
