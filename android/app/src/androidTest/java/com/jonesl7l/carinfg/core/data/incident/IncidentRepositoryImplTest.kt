package com.jonesl7l.carinfg.core.data.incident

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.core.data.mapper.toDomain
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.EntityTestHelper.INCIDENT_ID
import com.jonesl7l.carinfg.core.database.EntityTestHelper.assetEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.buildingEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.draftIncidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.now
import com.jonesl7l.carinfg.core.database.EntityTestHelper.photoEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.referenceEntities
import com.jonesl7l.carinfg.core.database.EntityTestHelper.siteEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.technicianEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.photo.PhotoUploadState
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.datasource.FakeIncidentRemoteDataSource
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import com.jonesl7l.carinfg.core.sync.FakeSyncScheduler
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncidentRepositoryImplTest {
    private val database: AppDatabase = inMemoryDatabase()
    private val dao = database.incidentDao()
    private val sessionStore = FakeSessionStore(initial = reporter)
    private val syncScheduler = FakeSyncScheduler()
    private val remoteDataSource = FakeIncidentRemoteDataSource()
    private val repository =
        IncidentRepositoryImpl(
            database = database,
            incidentDao = dao,
            referenceDao = database.referenceDao(),
            sessionStore = sessionStore,
            syncScheduler = syncScheduler,
            incidentRemoteDataSource = remoteDataSource,
        )
    private val undoWindow = 5.seconds

    @After
    fun tearDown() {
        database.close()
    }

    // region saveDraft
    @Test
    fun aNewIncidentIsSavedAsADraft() = runTest {
        val result = repository.saveDraft(
            incident = domain(entity = draftIncidentEntity()),
            now = now,
        )

        assertThat(result).isEqualTo(SaveResult.Saved)
        assertThat(stored()?.syncState).isEqualTo(SyncState.DRAFT)
        assertThat(stored()?.localRevision).isEqualTo(1L)
    }

    @Test
    fun aSubmittedIncidentCannotBeSavedAsADraft() = runTest {
        dao.upsertIncident(incident = incidentEntity(syncState = SyncState.SYNCED))

        val result = repository.saveDraft(
            incident = domain(entity = incidentEntity()),
            now = now,
        )

        assertThat(result).isEqualTo(SaveResult.NotAllowed)
    }
    // endregion

    // region submit
    @Test
    fun anIncompleteIncidentIsNotSubmitted() = runTest {
        val result = repository.submit(
            incident = domain(entity = draftIncidentEntity()),
            now = now,
        )

        assertThat(result).isInstanceOf<SaveResult.Invalid>()
        assertThat(stored()).isNull()
        assertThat(syncScheduler.syncRequests).isEqualTo(0)
    }

    @Test
    fun aCompleteDraftIsQueuedForCreate() = runTest {
        val draft = incidentEntity(
            syncState = SyncState.DRAFT,
            reportedAt = null,
        )
        dao.upsertIncident(incident = draft)

        val result = repository.submit(
            incident = domain(entity = draft),
            now = now,
        )

        assertThat(result).isEqualTo(SaveResult.Saved)
        assertThat(stored()?.syncState).isEqualTo(SyncState.PENDING_CREATE)
        assertThat(stored()?.reportedAt).isEqualTo(now)
        assertThat(syncScheduler.syncRequests).isEqualTo(1)
    }
    // endregion

    // region saveEdit
    @Test
    fun editingASyncedIncidentQueuesAnUpdateAndKeepsServerFields() = runTest {
        dao.upsertIncident(
            incident = incidentEntity(
                status = "ASSIGNED",
                serverVersion = 3,
            ),
        )
        val staleCopy = domain(
            entity = incidentEntity(
                title = "Edited title",
                status = "OPEN",
            ),
        )

        val result = repository.saveEdit(
            incident = staleCopy,
            now = now,
        )

        assertThat(result).isEqualTo(SaveResult.Saved)
        assertThat(stored()?.title).isEqualTo("Edited title")
        assertThat(stored()?.status).isEqualTo("ASSIGNED")
        assertThat(stored()?.serverVersion).isEqualTo(3)
        assertThat(stored()?.syncState).isEqualTo(SyncState.PENDING_UPDATE)
        assertThat(syncScheduler.syncRequests).isEqualTo(1)
    }

    @Test
    fun anotherTechniciansIncidentCannotBeEdited() = runTest {
        dao.upsertIncident(incident = incidentEntity(reporterId = "u2"))

        val result = repository.saveEdit(
            incident = domain(entity = incidentEntity(reporterId = "u2")),
            now = now,
        )

        assertThat(result).isEqualTo(SaveResult.NotAllowed)
        assertThat(syncScheduler.syncRequests).isEqualTo(0)
    }

    @Test
    fun anIncidentNeedingAttentionCannotBeEdited() = runTest {
        dao.upsertIncident(incident = incidentEntity(syncState = SyncState.NEEDS_ATTENTION))

        val result = repository.saveEdit(
            incident = domain(entity = incidentEntity()),
            now = now,
        )

        assertThat(result).isEqualTo(SaveResult.NotAllowed)
    }
    // endregion

    // region photos
    @Test
    fun aDraftSavesItsPhotosInListOrder() = runTest {
        val photos = listOf(
            newPhoto(id = "camera"),
            newPhoto(id = "picker"),
        )

        repository.saveDraft(
            incident = domain(
                entity = draftIncidentEntity(),
                photos = photos,
            ),
            now = now,
        )

        assertThat(storedPhotos().map { it.id }).containsExactly(
            "camera",
            "picker",
        )
        assertThat(storedPhotos().map { it.position }).containsExactly(
            0,
            1,
        )
    }

    @Test
    fun savingWithoutAPhotoRemovesItsRow() = runTest {
        val first = newPhoto(id = "first")
        val second = newPhoto(id = "second")
        dao.upsertIncident(incident = draftIncidentEntity())
        dao.upsertPhotos(
            photos = listOf(
                first,
                second.copy(position = 1),
            ),
        )

        repository.saveDraft(
            incident = domain(
                entity = draftIncidentEntity(),
                photos = listOf(second),
            ),
            now = now,
        )

        assertThat(storedPhotos()).containsExactly(second)
    }

    @Test
    fun editingKeepsUploadedPhotosAndAddsNewOnes() = runTest {
        val uploaded = photoEntity()
        val added = newPhoto(id = "added")
        dao.upsertIncident(incident = incidentEntity(reporterId = reporter.id))
        dao.upsertPhotos(photos = listOf(uploaded))

        repository.saveEdit(
            incident = domain(
                entity = incidentEntity(reporterId = reporter.id),
                photos = listOf(
                    added,
                    uploaded,
                ),
            ),
            now = now,
        )

        assertThat(storedPhotos()).containsExactly(
            added,
            uploaded.copy(position = 1),
        )
    }
    // endregion

    // region delete
    @Test
    fun deletingADraftRemovesIt() = runTest {
        dao.upsertIncident(incident = draftIncidentEntity())

        val result = repository.delete(
            id = INCIDENT_ID,
            now = now,
            undoWindow = undoWindow,
        )

        assertThat(result).isEqualTo(DeleteResult.Removed)
        assertThat(stored()).isNull()
        assertThat(syncScheduler.syncRequests).isEqualTo(0)
    }

    @Test
    fun deletingASyncedIncidentCanBeUndone() = runTest {
        dao.upsertIncident(incident = incidentEntity(syncState = SyncState.SYNCED))

        val result = repository.delete(
            id = INCIDENT_ID,
            now = now,
            undoWindow = undoWindow,
        )

        assertThat(result).isEqualTo(DeleteResult.PendingDelete(undoDeadline = now + undoWindow))
        assertThat(stored()?.syncState).isEqualTo(SyncState.PENDING_DELETE)
        assertThat(syncScheduler.syncRequests).isEqualTo(0)

        assertThat(
            repository.undoDelete(
                id = INCIDENT_ID,
                now = now,
            ),
        ).isTrue()
        assertThat(stored()?.syncState).isEqualTo(SyncState.SYNCED)
        assertThat(stored()?.deletedAt).isNull()
    }

    @Test
    fun dismissingTheUndoSendsTheDeleteNow() = runTest {
        dao.upsertIncident(incident = incidentEntity(syncState = SyncState.SYNCED))
        repository.delete(
            id = INCIDENT_ID,
            now = now,
            undoWindow = undoWindow,
        )

        repository.sendDeleteNow(
            id = INCIDENT_ID,
            now = now,
        )

        assertThat(stored()?.undoDeadline).isEqualTo(now)
        assertThat(syncScheduler.syncRequests).isEqualTo(1)
    }

    @Test
    fun anIncidentPastOpenCannotBeDeleted() = runTest {
        dao.upsertIncident(incident = incidentEntity(status = "ASSIGNED"))

        val result = repository.delete(
            id = INCIDENT_ID,
            now = now,
            undoWindow = undoWindow,
        )

        assertThat(result).isEqualTo(DeleteResult.NotAllowed)
    }
    // endregion

    // region observeIncidentList
    @Test
    fun theListLooksUpSiteAndBuildingNamesAndHidesPendingDeletes() = runTest {
        database.referenceDao().replaceAll(entities = referenceEntities())
        dao.upsertIncident(incident = incidentEntity(id = "shown"))
        dao.upsertIncident(
            incident = incidentEntity(
                id = "deleting",
                syncState = SyncState.PENDING_DELETE,
            ),
        )

        val items = repository.observeIncidentList().first()

        assertThat(items.map { it.id }).containsExactly("shown")
        assertThat(items.single().siteName).isEqualTo(siteEntity().name)
        assertThat(items.single().buildingName).isEqualTo(buildingEntity().name)
    }
    // endregion

    // region observeIncidentDetail
    @Test
    fun theDetailLooksUpReferenceNamesAndLeavesUnknownIdsEmpty() = runTest {
        database.referenceDao().replaceAll(entities = referenceEntities())
        dao.upsertIncident(incident = incidentEntity(roomId = "missing-room"))

        val names = repository.observeIncidentDetail(id = INCIDENT_ID).first()?.names

        assertThat(names?.siteName).isEqualTo(siteEntity().name)
        assertThat(names?.buildingName).isEqualTo(buildingEntity().name)
        assertThat(names?.assetCode).isEqualTo(assetEntity().code)
        assertThat(names?.reporterName).isEqualTo(technicianEntity().displayName)
        assertThat(names?.roomName).isNull()
    }
    // endregion

    // region resolve
    @Test
    fun discardRemovesAnIncidentDeletedRemotely() = runTest {
        dao.upsertIncident(incident = conflict(reason = ConflictReason.DELETED_REMOTELY))

        assertThat(repository.discard(id = INCIDENT_ID)).isEqualTo(ResolveResult.DONE)
        assertThat(stored()).isNull()
    }

    @Test
    fun resolutionsOnlyApplyToTheirOwnReason() = runTest {
        dao.upsertIncident(incident = conflict(reason = ConflictReason.VALIDATION))

        assertThat(repository.discard(id = INCIDENT_ID)).isEqualTo(ResolveResult.NOT_ALLOWED)
        assertThat(repository.keepIncident(id = INCIDENT_ID)).isEqualTo(ResolveResult.NOT_ALLOWED)
        assertThat(repository.discardMyChanges(id = INCIDENT_ID)).isEqualTo(ResolveResult.NOT_ALLOWED)
    }

    @Test
    fun submitAsNewQueuesACopyUnderANewId() = runTest {
        dao.upsertIncident(incident = conflict(reason = ConflictReason.DELETED_REMOTELY))

        val result = repository.submitAsNew(
            id = INCIDENT_ID,
            now = now,
        )

        val copy = dao.observeIncidents().first().single()
        assertThat(result).isEqualTo(ResolveResult.DONE)
        assertThat(copy.id).isNotEqualTo(INCIDENT_ID)
        assertThat(copy.title).isEqualTo(incidentEntity().title)
        assertThat(copy.syncState).isEqualTo(SyncState.PENDING_CREATE)
        assertThat(syncScheduler.syncRequests).isEqualTo(1)
    }

    @Test
    fun keepIncidentCancelsALockedDelete() = runTest {
        dao.upsertIncident(
            incident = conflict(
                reason = ConflictReason.STATUS_LOCKED,
                deletedAt = now,
            ),
        )

        assertThat(repository.keepIncident(id = INCIDENT_ID)).isEqualTo(ResolveResult.DONE)
        assertThat(stored()?.syncState).isEqualTo(SyncState.SYNCED)
        assertThat(stored()?.deletedAt).isNull()
    }

    @Test
    fun discardMyChangesNeedsTheServer() = runTest {
        dao.upsertIncident(incident = conflict(reason = ConflictReason.STATUS_LOCKED))
        remoteDataSource.incidentResults += ApiResult.Failure(error = ApiError.Network(cause = IOException()))

        assertThat(repository.discardMyChanges(id = INCIDENT_ID)).isEqualTo(ResolveResult.FAILED)
        assertThat(stored()?.conflictReason).isEqualTo(ConflictReason.STATUS_LOCKED)
    }

    @Test
    fun discardMyChangesOnAnIncidentGoneFromTheServerMarksItDeletedRemotely() = runTest {
        dao.upsertIncident(incident = conflict(reason = ConflictReason.STATUS_LOCKED))
        remoteDataSource.incidentResults += ApiResult.Failure(error = ApiError.NotFound)

        assertThat(repository.discardMyChanges(id = INCIDENT_ID)).isEqualTo(ResolveResult.DONE)
        assertThat(stored()?.conflictReason).isEqualTo(ConflictReason.DELETED_REMOTELY)
    }
    // endregion

    private fun conflict(
        reason: ConflictReason,
        deletedAt: Instant? = null,
    ) = incidentEntity(
        reporterId = reporter.id,
        syncState = SyncState.NEEDS_ATTENTION,
        conflictReason = reason,
        previousSyncState = deletedAt?.let { SyncState.SYNCED },
        deletedAt = deletedAt,
        deletedBy = deletedAt?.let { reporter.id },
    )

    private suspend fun stored(): IncidentEntity? = dao.getIncident(id = INCIDENT_ID)?.incident

    private suspend fun storedPhotos(): List<PhotoEntity> = dao.getIncident(id = INCIDENT_ID)?.photos.orEmpty().sortedBy { it.position }

    private fun newPhoto(id: String) = photoEntity(
        id = id,
        localPath = "incidents/$INCIDENT_ID/photos/$id.jpg",
        remoteUrl = null,
        uploadState = PhotoUploadState.PENDING,
    )

    private fun domain(
        entity: IncidentEntity,
        photos: List<PhotoEntity> = emptyList(),
    ): Incident = IncidentWithPhotos(
        incident = entity,
        photos = photos,
    ).toDomain()

    private companion object {
        val reporter =
            User(
                id = "u1",
                username = "tech1",
                displayName = "Tech One",
                role = UserRole.Technician,
            )
    }
}
