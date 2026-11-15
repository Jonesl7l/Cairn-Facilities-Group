package com.jonesl7l.carinfg.core.data.incident

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.core.data.mapper.toDomain
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.INCIDENT_ID
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.draftIncidentEntity
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.now
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.sync.FakeSyncScheduler
import kotlin.time.Duration.Companion.seconds
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
    private val repository =
        IncidentRepositoryImpl(
            database = database,
            incidentDao = dao,
            sessionStore = sessionStore,
            syncScheduler = syncScheduler,
        )
    private val undoWindow = 5.seconds

    @After
    fun tearDown() {
        database.close()
    }

    // region saveDraft
    @Test
    fun aNewIncidentIsSavedAsADraft() = runTest {
        val result = repository.saveDraft(incident = domain(entity = draftIncidentEntity()), now = now)

        assertThat(result).isEqualTo(DraftResult.Saved)
        assertThat(stored()?.syncState).isEqualTo(SyncState.DRAFT)
        assertThat(stored()?.localRevision).isEqualTo(1L)
    }

    @Test
    fun aSubmittedIncidentCannotBeSavedAsADraft() = runTest {
        dao.upsertIncident(incident = incidentEntity(syncState = SyncState.SYNCED))

        val result = repository.saveDraft(incident = domain(entity = incidentEntity()), now = now)

        assertThat(result).isEqualTo(DraftResult.NotADraft)
    }
    // endregion

    // region submit
    @Test
    fun anIncompleteIncidentIsNotSubmitted() = runTest {
        val result = repository.submit(incident = domain(entity = draftIncidentEntity()), now = now)

        assertThat(result).isInstanceOf<SubmitResult.Invalid>()
        assertThat(stored()).isNull()
        assertThat(syncScheduler.syncRequests).isEqualTo(0)
    }

    @Test
    fun aCompleteDraftIsQueuedForCreate() = runTest {
        val draft = incidentEntity(syncState = SyncState.DRAFT, reportedAt = null)
        dao.upsertIncident(incident = draft)

        val result = repository.submit(incident = domain(entity = draft), now = now)

        assertThat(result).isEqualTo(SubmitResult.Submitted)
        assertThat(stored()?.syncState).isEqualTo(SyncState.PENDING_CREATE)
        assertThat(stored()?.reportedAt).isEqualTo(now)
        assertThat(syncScheduler.syncRequests).isEqualTo(1)
    }
    // endregion

    // region saveEdit
    @Test
    fun editingASyncedIncidentQueuesAnUpdateAndKeepsServerFields() = runTest {
        dao.upsertIncident(incident = incidentEntity(status = "ASSIGNED", serverVersion = 3))
        val staleCopy = domain(entity = incidentEntity(title = "Edited title", status = "OPEN"))

        val result = repository.saveEdit(incident = staleCopy, now = now)

        assertThat(result).isEqualTo(EditResult.Saved)
        assertThat(stored()?.title).isEqualTo("Edited title")
        assertThat(stored()?.status).isEqualTo("ASSIGNED")
        assertThat(stored()?.serverVersion).isEqualTo(3)
        assertThat(stored()?.syncState).isEqualTo(SyncState.PENDING_UPDATE)
        assertThat(syncScheduler.syncRequests).isEqualTo(1)
    }

    @Test
    fun anotherTechniciansIncidentCannotBeEdited() = runTest {
        dao.upsertIncident(incident = incidentEntity(reporterId = "u2"))

        val result = repository.saveEdit(incident = domain(entity = incidentEntity(reporterId = "u2")), now = now)

        assertThat(result).isEqualTo(EditResult.NotAllowed)
        assertThat(syncScheduler.syncRequests).isEqualTo(0)
    }

    @Test
    fun anIncidentNeedingAttentionCannotBeEdited() = runTest {
        dao.upsertIncident(incident = incidentEntity(syncState = SyncState.NEEDS_ATTENTION))

        val result = repository.saveEdit(incident = domain(entity = incidentEntity()), now = now)

        assertThat(result).isEqualTo(EditResult.NotAllowed)
    }
    // endregion

    // region delete
    @Test
    fun deletingADraftRemovesIt() = runTest {
        dao.upsertIncident(incident = draftIncidentEntity())

        val result = repository.delete(id = INCIDENT_ID, now = now, undoWindow = undoWindow)

        assertThat(result).isEqualTo(DeleteResult.Removed)
        assertThat(stored()).isNull()
        assertThat(syncScheduler.delayedRequests).isEmpty()
    }

    @Test
    fun deletingASyncedIncidentCanBeUndone() = runTest {
        dao.upsertIncident(incident = incidentEntity(syncState = SyncState.SYNCED))

        val result = repository.delete(id = INCIDENT_ID, now = now, undoWindow = undoWindow)

        assertThat(result).isEqualTo(DeleteResult.PendingDelete(undoDeadline = now + undoWindow))
        assertThat(stored()?.syncState).isEqualTo(SyncState.PENDING_DELETE)
        assertThat(syncScheduler.delayedRequests).containsExactly(undoWindow)

        assertThat(repository.undoDelete(id = INCIDENT_ID, now = now)).isTrue()
        assertThat(stored()?.syncState).isEqualTo(SyncState.SYNCED)
        assertThat(stored()?.deletedAt).isNull()
    }

    @Test
    fun dismissingTheUndoSendsTheDeleteNow() = runTest {
        dao.upsertIncident(incident = incidentEntity(syncState = SyncState.SYNCED))
        repository.delete(id = INCIDENT_ID, now = now, undoWindow = undoWindow)

        repository.sendDeleteNow(id = INCIDENT_ID, now = now)

        assertThat(stored()?.undoDeadline).isEqualTo(now)
        assertThat(syncScheduler.syncRequests).isEqualTo(1)
    }

    @Test
    fun anIncidentPastOpenCannotBeDeleted() = runTest {
        dao.upsertIncident(incident = incidentEntity(status = "ASSIGNED"))

        val result = repository.delete(id = INCIDENT_ID, now = now, undoWindow = undoWindow)

        assertThat(result).isEqualTo(DeleteResult.NotAllowed)
    }
    // endregion

    private suspend fun stored(): IncidentEntity? = dao.getIncident(id = INCIDENT_ID)?.incident

    private fun domain(entity: IncidentEntity): Incident = IncidentWithPhotos(
        incident = entity,
        photos = emptyList(),
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
