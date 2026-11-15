package com.jonesl7l.carinfg.core.sync.push

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.database.EntityTestHelper.INCIDENT_ID
import com.jonesl7l.carinfg.core.database.EntityTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.photoEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity
import com.jonesl7l.carinfg.core.domain.model.photo.PhotoUploadState
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.ApiTestHelper.incidentDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.now
import com.jonesl7l.carinfg.core.network.ApiTestHelper.photoDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemFieldErrorDto
import com.jonesl7l.carinfg.core.network.datasource.FakeIncidentRemoteDataSource
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import com.jonesl7l.carinfg.core.sync.SyncRunResult
import com.jonesl7l.carinfg.core.sync.store.FakeSyncLocalStore
import java.io.IOException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PushEngineTest {
    private val remote = FakeIncidentRemoteDataSource()
    private val store = FakeSyncLocalStore()
    private val engine =
        PushEngine(
            incidentRemoteDataSource = remote,
            syncLocalStore = store,
            sessionStore = FakeSessionStore(initial = technician),
        )

    @Test
    fun `no session pushes nothing`() = runTest {
        val noSessionEngine =
            PushEngine(
                incidentRemoteDataSource = remote,
                syncLocalStore = store,
                sessionStore = FakeSessionStore(),
            )

        assertThat(noSessionEngine.push(now = now)).isEqualTo(SyncRunResult.Done)
    }

    @Test
    fun `a pending create is posted and stored as synced`() = runTest {
        stored(
            row = incidentEntity(
                syncState = SyncState.PENDING_CREATE,
                serverVersion = null,
            ),
        )
        remote.createResults += success(version = 1)

        val result = engine.push(now = now)

        assertThat(result).isEqualTo(SyncRunResult.Done)
        assertThat(row()?.syncState).isEqualTo(SyncState.SYNCED)
        assertThat(row()?.serverVersion).isEqualTo(1)
    }

    @Test
    fun `pending photos are uploaded first and sent with the create`() = runTest {
        val photo =
            photoEntity(
                remoteUrl = null,
                localPath = "photos/photo-0.jpg",
                uploadState = PhotoUploadState.PENDING,
            )
        stored(
            row = incidentEntity(
                syncState = SyncState.PENDING_CREATE,
                serverVersion = null,
            ),
            photos = listOf(photo),
        )
        remote.uploadResults += ApiResult.Success(
            value = photoDto(
                id = photo.id,
                url = "uploads/photo-0.jpg",
            ),
        )
        remote.createResults += success(version = 1)

        engine.push(now = now)

        assertThat(remote.uploadedPhotoIds).containsExactly(photo.id)
        assertThat(remote.createRequests.single().photos.map { it.url }).containsExactly("uploads/photo-0.jpg")
    }

    @Test
    fun `photos are not uploaded once the status no longer allows them`() = runTest {
        val photo =
            photoEntity(
                remoteUrl = null,
                localPath = "photos/photo-0.jpg",
                uploadState = PhotoUploadState.PENDING,
            )
        stored(
            row = incidentEntity(
                syncState = SyncState.PENDING_UPDATE,
                status = "RESOLVED",
            ),
            photos = listOf(photo),
        )

        engine.push(now = now)

        assertThat(remote.uploadedPhotoIds).isEmpty()
        assertNeedsAttention(reason = ConflictReason.STATUS_LOCKED)
    }

    @Test
    fun `a pending update is sent with its server version`() = runTest {
        stored(
            row = incidentEntity(
                syncState = SyncState.PENDING_UPDATE,
                serverVersion = 4,
            ),
        )
        remote.updateResults += success(version = 5)

        engine.push(now = now)

        assertThat(remote.updateVersions).containsExactly(4)
        assertThat(row()?.syncState).isEqualTo(SyncState.SYNCED)
    }

    @Test
    fun `a 412 retries once with the version from the server copy`() = runTest {
        stored(
            row = incidentEntity(
                syncState = SyncState.PENDING_UPDATE,
                serverVersion = 4,
            ),
        )
        remote.updateResults += versionConflict(current = incidentDto(version = 7))
        remote.updateResults += success(version = 8)

        engine.push(now = now)

        assertThat(remote.updateVersions).containsExactly(4, 7)
        assertThat(remote.incidentRequests).isEmpty()
        assertThat(row()?.serverVersion).isEqualTo(8)
    }

    @Test
    fun `a 412 without a server copy fetches the incident before retrying`() = runTest {
        stored(
            row = incidentEntity(
                syncState = SyncState.PENDING_UPDATE,
                serverVersion = 4,
            ),
        )
        remote.updateResults += versionConflict(current = null)
        remote.incidentResults += success(version = 9)
        remote.updateResults += success(version = 10)

        engine.push(now = now)

        assertThat(remote.incidentRequests).containsExactly(INCIDENT_ID)
        assertThat(remote.updateVersions).containsExactly(4, 9)
    }

    @Test
    fun `a second 412 leaves the incident pending for the next pass`() = runTest {
        stored(
            row = incidentEntity(
                syncState = SyncState.PENDING_UPDATE,
                serverVersion = 4,
            ),
        )
        remote.updateResults += versionConflict(current = incidentDto(version = 7))
        remote.updateResults += versionConflict(current = incidentDto(version = 8))

        val result = engine.push(now = now)

        assertThat(result).isEqualTo(SyncRunResult.Done)
        assertThat(row()?.syncState).isEqualTo(SyncState.PENDING_UPDATE)
        assertThat(store.failures).isEmpty()
    }

    @Test
    fun `an update the status no longer allows needs attention without a call`() = runTest {
        stored(
            row = incidentEntity(
                syncState = SyncState.PENDING_UPDATE,
                status = "IN_PROGRESS",
            ),
        )

        engine.push(now = now)

        assertThat(remote.updateVersions).isEmpty()
        assertNeedsAttention(reason = ConflictReason.STATUS_LOCKED)
    }

    @Test
    fun `an incident missing a required field needs attention without a call`() = runTest {
        stored(
            row = incidentEntity(
                syncState = SyncState.PENDING_CREATE,
                serverVersion = null,
                severity = null,
            ),
        )

        engine.push(now = now)

        assertThat(remote.createRequests).isEmpty()
        assertNeedsAttention(reason = ConflictReason.VALIDATION)
    }

    @Test
    fun `a 404 on an update submits it again as a new report`() = runTest {
        stored(
            row = incidentEntity(
                syncState = SyncState.PENDING_UPDATE,
                serverVersion = 4,
            ),
        )
        remote.updateResults += ApiResult.Failure(error = ApiError.NotFound)
        remote.createResults += success(version = 1)

        engine.push(now = now)

        assertThat(remote.createRequests.size).isEqualTo(1)
        assertThat(row()?.syncState).isEqualTo(SyncState.SYNCED)
    }

    @Test
    fun `a delete that never reached the server is removed without a call`() = runTest {
        stored(row = pendingDelete(serverVersion = null))

        engine.push(now = now)

        assertThat(remote.deleteVersions).isEmpty()
        assertThat(row()).isNull()
    }

    @Test
    fun `a pending delete is sent and removed locally`() = runTest {
        stored(row = pendingDelete(serverVersion = 3))
        remote.deleteResults += success(version = 4)

        engine.push(now = now)

        assertThat(remote.deleteVersions).containsExactly(3)
        assertThat(row()).isNull()
    }

    @Test
    fun `a delete the status no longer allows needs attention without a call`() = runTest {
        stored(row = pendingDelete(serverVersion = 3).copy(status = "ASSIGNED"))

        engine.push(now = now)

        assertThat(remote.deleteVersions).isEmpty()
        assertNeedsAttention(reason = ConflictReason.STATUS_LOCKED)
    }

    @Test
    fun `a 422 needs attention and the pass carries on`() = runTest {
        stored(
            row = incidentEntity(
                id = "first",
                syncState = SyncState.PENDING_UPDATE,
                localUpdatedAt = now - 2.minutes,
            ),
        )
        stored(
            row = incidentEntity(
                id = "second",
                syncState = SyncState.PENDING_UPDATE,
                localUpdatedAt =
                    now - 1.minutes,
            ),
        )
        remote.updateResults +=
            ApiResult.Failure(error = ApiError.ValidationFailed(fieldErrors = listOf(problemFieldErrorDto())))
        remote.updateResults += success(
            id = "second",
            version = 2,
        )

        val result = engine.push(now = now)

        assertThat(result).isEqualTo(SyncRunResult.Done)
        assertThat(row(id = "first")?.conflictReason).isEqualTo(ConflictReason.VALIDATION)
        assertThat(row(id = "first")?.lastSyncError).isEqualTo("ValidationFailed")
        assertThat(row(id = "second")?.syncState).isEqualTo(SyncState.SYNCED)
    }

    @Test
    fun `an error that points to an app bug is recorded and the pass carries on`() = runTest {
        stored(
            row = incidentEntity(
                id = "first",
                syncState = SyncState.PENDING_UPDATE,
                localUpdatedAt = now - 2.minutes,
            ),
        )
        stored(
            row = incidentEntity(
                id = "second",
                syncState = SyncState.PENDING_UPDATE,
                localUpdatedAt =
                    now - 1.minutes,
            ),
        )
        remote.updateResults += ApiResult.Failure(error = ApiError.Forbidden)
        remote.updateResults += success(
            id = "second",
            version = 2,
        )

        engine.push(now = now)

        assertThat(row(id = "first")).isNotNull().prop(IncidentEntity::syncAttempts).isEqualTo(1)
        assertThat(row(id = "first")?.syncState).isEqualTo(SyncState.PENDING_UPDATE)
        assertThat(row(id = "second")?.syncState).isEqualTo(SyncState.SYNCED)
    }

    @Test
    fun `a network error stops the pass and counts the attempt`() = runTest {
        stored(
            row = incidentEntity(
                id = "first",
                syncState = SyncState.PENDING_UPDATE,
                localUpdatedAt = now - 2.minutes,
            ),
        )
        stored(
            row = incidentEntity(
                id = "second",
                syncState = SyncState.PENDING_UPDATE,
                localUpdatedAt =
                    now - 1.minutes,
            ),
        )
        remote.updateResults += ApiResult.Failure(error = ApiError.Network(cause = IOException()))

        val result = engine.push(now = now)

        assertThat(result).isEqualTo(SyncRunResult.RetryWithBackoff)
        assertThat(store.failures).containsExactly("first" to "Network")
        assertThat(remote.updateVersions.size).isEqualTo(1)
    }

    @Test
    fun `a 503 stops the pass to retry with backoff`() = runTest {
        stored(row = incidentEntity(syncState = SyncState.PENDING_UPDATE))
        remote.updateResults += ApiResult.Failure(error = ApiError.ServiceUnavailable(retryAfter = 30.seconds))

        assertThat(engine.push(now = now)).isEqualTo(SyncRunResult.RetryWithBackoff)
    }

    @Test
    fun `a 401 stops the pass as session expired`() = runTest {
        stored(row = incidentEntity(syncState = SyncState.PENDING_UPDATE))
        remote.updateResults += ApiResult.Failure(error = ApiError.Unauthenticated)

        assertThat(engine.push(now = now)).isEqualTo(SyncRunResult.SessionExpired)
        assertThat(row()?.syncState).isEqualTo(SyncState.PENDING_UPDATE)
    }

    private fun stored(
        row: IncidentEntity,
        photos: List<PhotoEntity> = emptyList(),
    ) {
        store.incidents[row.id] = IncidentWithPhotos(
            incident = row,
            photos = photos,
        )
    }

    private fun row(id: String = INCIDENT_ID): IncidentEntity? = store.incidents[id]?.incident

    private fun assertNeedsAttention(reason: ConflictReason) {
        assertThat(row()?.syncState).isEqualTo(SyncState.NEEDS_ATTENTION)
        assertThat(row()?.conflictReason).isEqualTo(reason)
    }

    private fun pendingDelete(serverVersion: Int?) = incidentEntity(
        syncState = SyncState.PENDING_DELETE,
        serverVersion = serverVersion,
        deletedAt = now,
        deletedBy = technician.id,
    )

    private fun success(
        id: String = INCIDENT_ID,
        version: Int,
    ) = ApiResult.Success(
        value = incidentDto(
            id = id,
            version = version,
            photos = emptyList(),
        ),
    )

    private fun versionConflict(current: IncidentDto?) = ApiResult.Failure(error = ApiError.VersionConflict(current = current))

    private companion object {
        val technician =
            User(
                id = "u1",
                username = "tech1",
                displayName = "Tech One",
                role = UserRole.Technician,
            )
    }
}
