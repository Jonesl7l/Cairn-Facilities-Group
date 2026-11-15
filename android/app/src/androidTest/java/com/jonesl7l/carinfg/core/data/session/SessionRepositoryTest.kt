package com.jonesl7l.carinfg.core.data.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.referenceEntities
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.syncMetaEntity
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.datasource.FakeAuthRemoteDataSource
import com.jonesl7l.carinfg.core.network.dto.user.UserDto
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import com.jonesl7l.carinfg.core.sync.FakeSyncScheduler
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionRepositoryTest {
    private val database: AppDatabase = inMemoryDatabase()
    private val sessionStore = FakeSessionStore()
    private val syncScheduler = FakeSyncScheduler()
    private val authRemoteDataSource = FakeAuthRemoteDataSource(result = ApiResult.Success(value = technicianDto))
    private val repository =
        SessionRepository(
            authRemoteDataSource = authRemoteDataSource,
            sessionStore = sessionStore,
            incidentDao = database.incidentDao(),
            syncMetaDao = database.syncMetaDao(),
            syncScheduler = syncScheduler,
        )

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aTechnicianLogsInAndTheSessionIsSaved() = runTest {
        val result = repository.login(username = "tech1", password = "password")

        assertThat(result).isEqualTo(LoginResult.Success(user = technician))
        assertThat(repository.observeUser().first()).isEqualTo(technician)
        assertThat(syncScheduler.syncRequests).isEqualTo(1)
    }

    @Test
    fun wrongCredentialsAreInvalidCredentials() = runTest {
        authRemoteDataSource.result = ApiResult.Failure(error = ApiError.InvalidCredentials)

        assertThat(repository.login(username = "tech1", password = "wrong")).isEqualTo(LoginResult.InvalidCredentials)
        assertThat(repository.observeUser().first()).isNull()
    }

    @Test
    fun anOperatorIsRejectedWithoutASession() = runTest {
        authRemoteDataSource.result = ApiResult.Success(value = technicianDto.copy(role = "OPERATOR"))

        assertThat(repository.login(username = "operator", password = "password")).isEqualTo(LoginResult.NotTechnician)
        assertThat(repository.observeUser().first()).isNull()
        assertThat(syncScheduler.syncRequests).isEqualTo(0)
    }

    @Test
    fun otherFailuresCarryTheirError() = runTest {
        val error = ApiError.Network(cause = IOException("offline"))
        authRemoteDataSource.result = ApiResult.Failure(error = error)

        val result = repository.login(username = "tech1", password = "password")

        assertThat(result).isEqualTo(LoginResult.Failed(error = error))
    }

    @Test
    fun theUnsyncedCountIncludesEverythingThatIsNotSynced() = runTest {
        val incidentDao = database.incidentDao()
        incidentDao.upsertIncident(incident = incidentEntity(id = "synced", syncState = SyncState.SYNCED))
        incidentDao.upsertIncident(incident = incidentEntity(id = "draft", syncState = SyncState.DRAFT))
        incidentDao.upsertIncident(incident = incidentEntity(id = "pending", syncState = SyncState.PENDING_UPDATE))
        incidentDao.upsertIncident(incident = incidentEntity(id = "attention", syncState = SyncState.NEEDS_ATTENTION))

        assertThat(repository.observeUnsyncedCount().first()).isEqualTo(3)
    }

    @Test
    fun logoutClearsTheUsersDataAndSessionButKeepsReferenceData() = runTest {
        repository.login(username = "tech1", password = "password")
        database.incidentDao().upsertIncident(incident = incidentEntity())
        database.syncMetaDao().upsert(meta = syncMetaEntity())
        database.referenceDao().replaceAll(entities = referenceEntities())

        repository.logout()

        assertThat(database.incidentDao().getIncident(id = incidentEntity().id)).isNull()
        assertThat(database.syncMetaDao().get()).isNull()
        assertThat(database.referenceDao().isEmpty()).isFalse()
        assertThat(repository.observeUser().first()).isNull()
        assertThat(syncScheduler.cancelled).isTrue()
    }

    private companion object {
        val technicianDto =
            UserDto(
                id = "u1",
                username = "tech1",
                displayName = "Tech One",
                role = "TECHNICIAN",
            )
        val technician =
            User(
                id = "u1",
                username = "tech1",
                displayName = "Tech One",
                role = UserRole.Technician,
            )
    }
}
