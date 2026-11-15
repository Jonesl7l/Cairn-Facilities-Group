package com.jonesl7l.carinfg.core.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.INCIDENT_ID
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.draftIncidentEntity
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.photoEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncidentDaoTest {
    private val database: AppDatabase = inMemoryDatabase()
    private val dao = database.incidentDao()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aFullIncidentRoundTripsWithItsPhotos() = runTest {
        val incident = incidentEntity()
        val photos = listOf(photoEntity(position = 0), photoEntity(position = 1))
        dao.upsertIncident(incident = incident)
        dao.upsertPhotos(photos = photos)

        val stored = dao.getIncident(id = INCIDENT_ID)

        assertThat(stored).isNotNull().prop(IncidentWithPhotos::incident).isEqualTo(incident)
        assertThat(stored?.photos?.sortedBy { it.position }).isEqualTo(photos)
    }

    @Test
    fun aDraftWithEveryOptionalFieldEmptyRoundTrips() = runTest {
        val draft = draftIncidentEntity()
        dao.upsertIncident(incident = draft)

        assertThat(dao.getIncident(id = INCIDENT_ID)?.incident).isEqualTo(draft)
    }

    @Test
    fun upsertUpdatesTheIncidentAndKeepsItsPhotos() = runTest {
        dao.upsertIncident(incident = incidentEntity())
        dao.upsertPhotos(photos = listOf(photoEntity()))
        val edited =
            incidentEntity(
                title = "Edited title",
                syncState = SyncState.PENDING_UPDATE,
                localRevision = 1,
            )

        dao.upsertIncident(incident = edited)

        val stored = dao.getIncident(id = INCIDENT_ID)
        assertThat(stored?.incident).isEqualTo(edited)
        assertThat(stored?.photos).isEqualTo(listOf(photoEntity()))
    }

    @Test
    fun deletingAnIncidentDeletesItsPhotos() = runTest {
        dao.upsertIncident(incident = incidentEntity())
        dao.upsertPhotos(photos = listOf(photoEntity()))

        dao.deleteIncident(id = INCIDENT_ID)
        dao.upsertIncident(incident = incidentEntity())

        assertThat(dao.getIncident(id = INCIDENT_ID)?.photos).isNotNull().isEmpty()
    }

    @Test
    fun observeEmitsNullThenTheIncident() = runTest {
        assertThat(dao.observeIncident(id = INCIDENT_ID).first()).isNull()

        dao.upsertIncident(incident = incidentEntity())

        assertThat(dao.observeIncident(id = INCIDENT_ID).first()?.incident).isEqualTo(incidentEntity())
    }

    @Test
    fun deleteAllRemovesEveryIncidentAndItsPhotos() = runTest {
        dao.upsertIncident(incident = incidentEntity())
        dao.upsertPhotos(photos = listOf(photoEntity()))

        dao.deleteAll()
        dao.upsertIncident(incident = incidentEntity())

        assertThat(dao.getIncident(id = INCIDENT_ID)?.photos).isNotNull().isEmpty()
    }

    @Test
    fun observeUnsyncedCountFollowsSyncState() = runTest {
        dao.upsertIncident(incident = incidentEntity(syncState = SyncState.PENDING_CREATE))
        assertThat(dao.observeUnsyncedCount().first()).isEqualTo(1)

        dao.upsertIncident(incident = incidentEntity(syncState = SyncState.SYNCED))
        assertThat(dao.observeUnsyncedCount().first()).isEqualTo(0)
    }
}
