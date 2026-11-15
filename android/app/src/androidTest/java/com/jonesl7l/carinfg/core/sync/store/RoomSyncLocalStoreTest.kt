package com.jonesl7l.carinfg.core.sync.store

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.now
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.photoEntity
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.syncMetaEntity
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.sync.pull.PullAction
import kotlin.time.Duration.Companion.days
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
}
