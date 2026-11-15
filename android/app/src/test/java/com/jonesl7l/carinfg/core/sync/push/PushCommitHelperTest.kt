package com.jonesl7l.carinfg.core.sync.push

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.jonesl7l.carinfg.core.data.mapper.toEntity
import com.jonesl7l.carinfg.core.data.mapper.toPhotoEntities
import com.jonesl7l.carinfg.core.database.EntityTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.photoEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.network.ApiTestHelper.incidentDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.now
import com.jonesl7l.carinfg.core.network.ApiTestHelper.photoDto
import org.junit.Test

class PushCommitHelperTest {
    private val remote =
        incidentDto(
            referenceNumber = "INC-2026-00007",
            status = "ASSIGNED",
            version = 3,
            changeSeq = 30,
            photos = listOf(photoDto(id = "photo-0")),
        )

    @Test
    fun `an unchanged row becomes the synced server copy`() {
        val row = incidentEntity(
            syncState = SyncState.PENDING_CREATE,
            localRevision = 2,
        )

        val commit = commit(
            row = row,
            basedOnRevision = 2,
        )

        val expected = remote.toEntity().copy(
            localRevision = 2,
            localUpdatedAt = row.localUpdatedAt,
        )
        assertThat(commit.entity).isEqualTo(expected)
        assertThat(commit.replacementPhotos).isEqualTo(remote.toPhotoEntities())
    }

    @Test
    fun `an unchanged row keeps the local path of its uploaded photos`() {
        val row = incidentEntity(
            syncState = SyncState.PENDING_UPDATE,
            localRevision = 1,
        )
        val photo = photoEntity(
            id = "photo-0",
            localPath = "photos/photo-0.jpg",
        )

        val commit = commit(
            row = row,
            basedOnRevision = 1,
            photos = listOf(photo),
        )

        assertThat(commit.replacementPhotos?.single()?.localPath).isEqualTo("photos/photo-0.jpg")
    }

    @Test
    fun `a create edited during the push becomes a pending update with the new version`() {
        val row = incidentEntity(
            syncState = SyncState.PENDING_CREATE,
            title = "Edited",
            localRevision = 3,
        )

        val commit = commit(
            row = row,
            basedOnRevision = 2,
        )

        assertThat(commit.entity.syncState).isEqualTo(SyncState.PENDING_UPDATE)
        assertThat(commit.entity.title).isEqualTo("Edited")
        assertThat(commit.entity.serverVersion).isEqualTo(3)
        assertThat(commit.entity.referenceNumber).isEqualTo("INC-2026-00007")
        assertThat(commit.replacementPhotos).isNull()
    }

    @Test
    fun `an update edited during the push stays a pending update with the new version`() {
        val row =
            incidentEntity(
                syncState = SyncState.PENDING_UPDATE,
                localRevision = 5,
                lastSyncError = "Network",
                syncAttempts = 2,
            )

        val commit = commit(
            row = row,
            basedOnRevision = 4,
        )

        assertThat(commit.entity.syncState).isEqualTo(SyncState.PENDING_UPDATE)
        assertThat(commit.entity.serverVersion).isEqualTo(3)
        assertThat(commit.entity.localRevision).isEqualTo(5)
        assertThat(commit.entity.lastSyncError).isNull()
        assertThat(commit.entity.syncAttempts).isEqualTo(0)
    }

    @Test
    fun `a delete made during the push stays pending and keeps its delete fields`() {
        val row =
            incidentEntity(
                syncState = SyncState.PENDING_DELETE,
                deletedAt = now,
                deletedBy = "tech1",
                localRevision = 2,
            )

        val commit = commit(
            row = row,
            basedOnRevision = 1,
        )

        assertThat(commit.entity.syncState).isEqualTo(SyncState.PENDING_DELETE)
        assertThat(commit.entity.serverVersion).isEqualTo(3)
        assertThat(commit.entity.deletedAt).isEqualTo(now)
        assertThat(commit.entity.deletedBy).isEqualTo("tech1")
    }

    private fun commit(
        row: IncidentEntity,
        basedOnRevision: Long,
        photos: List<PhotoEntity> = emptyList(),
    ) = PushCommitHelper.commit(
        current = IncidentWithPhotos(
            incident = row,
            photos = photos,
        ),
        remote = remote,
        basedOnRevision = basedOnRevision,
    )
}
