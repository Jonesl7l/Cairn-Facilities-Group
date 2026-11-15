package com.jonesl7l.carinfg.core.sync.store

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.INCIDENT_ID
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.now
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.photoEntity
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.syncMetaEntity
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import com.jonesl7l.carinfg.core.domain.model.photo.PhotoUploadState
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.sync.pull.PullAction
import com.jonesl7l.carinfg.core.sync.push.PushCommit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomSyncLocalStoreTest {
    private val database: AppDatabase = inMemoryDatabase()
    private val incidentDao = database.incidentDao()
    private val store =
        RoomSyncLocalStore(
            database = database,
            incidentDao = incidentDao,
            syncMetaDao = database.syncMetaDao(),
        )

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun syncMetaIsTheInitialRowUntilWritten() = runTest {
        assertThat(store.getSyncMeta()).isEqualTo(SyncMetaEntity.initial)
    }

    @Test
    fun incidentsAreReturnedById() = runTest {
        incidentDao.upsertIncident(incident = incidentEntity(id = "a"))
        incidentDao.upsertIncident(incident = incidentEntity(id = "b"))

        assertThat(store.getIncidents(ids = listOf("a", "missing")).keys).containsOnly("a")
    }

    @Test
    fun aPageIsAppliedWithItsCursor() = runTest {
        incidentDao.upsertIncident(incident = incidentEntity(id = "gone", localRevision = 1))
        val meta = syncMetaEntity(lastChangeSeq = 50)
        val actions =
            listOf(
                PullAction.Upsert(
                    entity = incidentEntity(id = "new"),
                    replacementPhotos = listOf(photoEntity(incidentId = "new")),
                    basedOnRevision = null,
                ),
                PullAction.Delete(id = "gone", basedOnRevision = 1),
                PullAction.Skip,
            )

        store.applyPullPage(actions = actions, meta = meta)

        assertThat(incidentDao.getIncident(id = "new")?.photos?.size).isEqualTo(1)
        assertThat(incidentDao.getIncident(id = "gone")).isNull()
        assertThat(store.getSyncMeta()).isEqualTo(meta)
    }

    @Test
    fun replacementPhotosReplaceAndNullKeepsTheLocalPhotos() = runTest {
        incidentDao.upsertIncident(incident = incidentEntity(id = "a"))
        incidentDao.upsertPhotos(photos = listOf(photoEntity(id = "old", incidentId = "a")))
        incidentDao.upsertIncident(incident = incidentEntity(id = "b"))
        incidentDao.upsertPhotos(photos = listOf(photoEntity(id = "kept", incidentId = "b")))
        val actions =
            listOf(
                PullAction.Upsert(
                    entity = incidentEntity(id = "a"),
                    replacementPhotos = listOf(photoEntity(id = "new", incidentId = "a")),
                    basedOnRevision = 0,
                ),
                PullAction.Upsert(
                    entity = incidentEntity(id = "b", title = "Server title"),
                    replacementPhotos = null,
                    basedOnRevision = 0,
                ),
            )

        store.applyPullPage(actions = actions, meta = syncMetaEntity())

        assertThat(incidentDao.getIncident(id = "a")?.photos?.map { it.id }).isEqualTo(listOf("new"))
        assertThat(incidentDao.getIncident(id = "b")?.photos?.map { it.id }).isEqualTo(listOf("kept"))
        assertThat(incidentDao.getIncident(id = "b")?.incident?.title).isEqualTo("Server title")
    }

    @Test
    fun anActionIsSkippedWhenTheRowChangedSinceItWasRead() = runTest {
        incidentDao.upsertIncident(incident = incidentEntity(title = "Edited locally", localRevision = 2))
        val stale =
            PullAction.Upsert(
                entity = incidentEntity(title = "Server title", localRevision = 1),
                replacementPhotos = null,
                basedOnRevision = 1,
            )

        store.applyPullPage(actions = listOf(stale), meta = syncMetaEntity())

        assertThat(incidentDao.getIncident(id = incidentEntity().id)?.incident?.title).isEqualTo("Edited locally")
    }

    @Test
    fun aServerResetRemovesSyncedRowsKeepsPendingOnesAndResetsTheCursor() = runTest {
        incidentDao.upsertIncident(incident = incidentEntity(id = "synced", syncState = SyncState.SYNCED))
        incidentDao.upsertIncident(incident = incidentEntity(id = "pending", syncState = SyncState.PENDING_UPDATE))
        database.syncMetaDao().upsert(meta = syncMetaEntity(lastChangeSeq = 90, databaseId = "db-1"))

        store.resetForNewDatabase(databaseId = "db-2")

        assertThat(incidentDao.getIncident(id = "synced")).isNull()
        assertThat(incidentDao.getIncident(id = "pending")).isNotNull()
        assertThat(store.getSyncMeta()).isEqualTo(SyncMetaEntity.initial.copy(databaseId = "db-2"))
    }

    @Test
    fun pruningRemovesOnlyOtherTechniciansOldClosedSyncedRows() = runTest {
        val old = now - 31.days
        val rows =
            listOf(
                incidentEntity(id = "mine", reporterId = "u1", status = "CLOSED", updatedAt = old),
                incidentEntity(id = "othersOpen", reporterId = "u2", status = "ASSIGNED", updatedAt = old),
                incidentEntity(id = "othersRecent", reporterId = "u2", status = "CLOSED", updatedAt = now),
                incidentEntity(id = "othersOld", reporterId = "u2", status = "CLOSED", updatedAt = old),
                incidentEntity(
                    id = "othersOldPending",
                    reporterId = "u2",
                    status = "CLOSED",
                    updatedAt = old,
                    syncState = SyncState.NEEDS_ATTENTION,
                ),
            )
        rows.forEach { incidentDao.upsertIncident(incident = it) }

        store.pruneOutOfScope(userId = "u1", updatedBefore = now - 30.days)

        val remaining = store.getIncidents(ids = rows.map { it.id }).keys
        assertThat(remaining).containsOnly("mine", "othersOpen", "othersRecent", "othersOldPending")
    }

    @Test
    fun pendingIncidentsAreOldestFirstAndDeletesWaitForTheirUndoDeadline() = runTest {
        val rows =
            listOf(
                incidentEntity(id = "update", syncState = SyncState.PENDING_UPDATE, localUpdatedAt = now - 1.minutes),
                incidentEntity(id = "create", syncState = SyncState.PENDING_CREATE, localUpdatedAt = now - 2.minutes),
                incidentEntity(
                    id = "dueDelete",
                    syncState = SyncState.PENDING_DELETE,
                    undoDeadline = now,
                    localUpdatedAt = now - 3.minutes,
                ),
                incidentEntity(
                    id = "waitingDelete",
                    syncState = SyncState.PENDING_DELETE,
                    undoDeadline = now + 5.seconds,
                ),
                incidentEntity(id = "synced", syncState = SyncState.SYNCED),
                incidentEntity(id = "draft", syncState = SyncState.DRAFT),
                incidentEntity(id = "attention", syncState = SyncState.NEEDS_ATTENTION),
            )
        rows.forEach { incidentDao.upsertIncident(incident = it) }

        val pending = store.getPendingIncidents(now = now).map { it.incident.id }

        assertThat(pending).containsExactly("dueDelete", "create", "update")
    }

    @Test
    fun anUploadedPhotoStoresItsUrl() = runTest {
        incidentDao.upsertIncident(incident = incidentEntity())
        incidentDao.upsertPhotos(
            photos = listOf(photoEntity(remoteUrl = null, uploadState = PhotoUploadState.PENDING)),
        )

        store.markPhotoUploaded(photoId = photoEntity().id, remoteUrl = "uploads/photo-0.jpg")

        val photo = incidentDao.getIncident(id = INCIDENT_ID)?.photos?.single()
        assertThat(photo?.remoteUrl).isEqualTo("uploads/photo-0.jpg")
        assertThat(photo?.uploadState).isEqualTo(PhotoUploadState.UPLOADED)
    }

    @Test
    fun commitPushWritesTheMergedRowAndReplacesPhotos() = runTest {
        incidentDao.upsertIncident(incident = incidentEntity(syncState = SyncState.PENDING_CREATE))
        incidentDao.upsertPhotos(photos = listOf(photoEntity(id = "old")))
        val synced = incidentEntity(syncState = SyncState.SYNCED, serverVersion = 2)

        store.commitPush(id = INCIDENT_ID) {
            PushCommit(entity = synced, replacementPhotos = listOf(photoEntity(id = "new")))
        }

        val stored = incidentDao.getIncident(id = INCIDENT_ID)
        assertThat(stored?.incident).isEqualTo(synced)
        assertThat(stored?.photos?.map { it.id }).isEqualTo(listOf("new"))
    }

    @Test
    fun commitPushDoesNothingWhenTheRowIsGone() = runTest {
        store.commitPush(id = INCIDENT_ID) { PushCommit(entity = incidentEntity(), replacementPhotos = null) }

        assertThat(incidentDao.getIncident(id = INCIDENT_ID)).isNull()
    }

    @Test
    fun aPushedDeleteIsRemovedOnlyWhenUnchanged() = runTest {
        incidentDao.upsertIncident(
            incident = incidentEntity(id = "a", syncState = SyncState.PENDING_DELETE, localRevision = 1),
        )
        incidentDao.upsertIncident(
            incident = incidentEntity(id = "b", syncState = SyncState.PENDING_DELETE, localRevision = 2),
        )

        store.removePushed(id = "a", basedOnRevision = 1)
        store.removePushed(id = "b", basedOnRevision = 1)

        assertThat(incidentDao.getIncident(id = "a")).isNull()
        assertThat(incidentDao.getIncident(id = "b")).isNotNull()
    }

    @Test
    fun needsAttentionIsMarkedOnlyWhenUnchanged() = runTest {
        incidentDao.upsertIncident(
            incident = incidentEntity(id = "a", syncState = SyncState.PENDING_UPDATE, localRevision = 1),
        )
        incidentDao.upsertIncident(
            incident = incidentEntity(id = "b", syncState = SyncState.PENDING_UPDATE, localRevision = 2),
        )

        listOf("a", "b").forEach { id ->
            store.markNeedsAttention(
                id = id,
                reason = ConflictReason.STATUS_LOCKED,
                error = "StatusLocked",
                basedOnRevision = 1,
            )
        }

        val marked = incidentDao.getIncident(id = "a")?.incident
        assertThat(marked?.syncState).isEqualTo(SyncState.NEEDS_ATTENTION)
        assertThat(marked?.conflictReason).isEqualTo(ConflictReason.STATUS_LOCKED)
        assertThat(marked?.lastSyncError).isEqualTo("StatusLocked")
        assertThat(incidentDao.getIncident(id = "b")?.incident?.syncState).isEqualTo(SyncState.PENDING_UPDATE)
    }

    @Test
    fun aFailureCountsTheAttemptAndKeepsTheRowPending() = runTest {
        incidentDao.upsertIncident(incident = incidentEntity(syncState = SyncState.PENDING_UPDATE, syncAttempts = 2))

        store.recordFailure(id = INCIDENT_ID, error = "Network")

        val row = incidentDao.getIncident(id = INCIDENT_ID)?.incident
        assertThat(row?.syncAttempts).isEqualTo(3)
        assertThat(row?.lastSyncError).isEqualTo("Network")
        assertThat(row?.syncState).isEqualTo(SyncState.PENDING_UPDATE)
    }

    @Test
    fun anAnnouncedChangeOnlyEverRaisesTheStoredValue() = runTest {
        store.raiseAnnouncedChangeSeq(changeSeq = 7)
        store.raiseAnnouncedChangeSeq(changeSeq = 5)

        assertThat(store.getSyncMeta().announcedChangeSeq).isEqualTo(7L)
    }
}
