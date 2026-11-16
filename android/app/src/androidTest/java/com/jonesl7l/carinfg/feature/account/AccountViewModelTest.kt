package com.jonesl7l.carinfg.feature.account

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.MainDispatcherRule
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.data.session.SessionRepository
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.EntityTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.datasource.FakeAuthRemoteDataSource
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import com.jonesl7l.carinfg.core.sync.FakeSyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher = StandardTestDispatcher())

    private val database: AppDatabase = inMemoryDatabase()
    private val sessionStore = FakeSessionStore(initial = technician)
    private val syncScheduler = FakeSyncScheduler()
    private val viewModel by lazy {
        AccountViewModel(
            sessionRepository =
                SessionRepository(
                    authRemoteDataSource = FakeAuthRemoteDataSource(result = ApiResult.Failure(error = ApiError.InvalidCredentials)),
                    sessionStore = sessionStore,
                    incidentDao = database.incidentDao(),
                    syncMetaDao = database.syncMetaDao(),
                    syncScheduler = syncScheduler,
                ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun theSheetShowsTheUserAndTheirUnsyncedCount() = runTest {
        database.incidentDao().upsertIncident(
            incident = incidentEntity(
                id = "pending",
                syncState = SyncState.PENDING_UPDATE,
            ),
        )
        database.incidentDao().upsertIncident(incident = incidentEntity(id = "synced"))

        val state = viewModel.uiState.first { it.displayName.isNotEmpty() }

        assertThat(state.displayName).isEqualTo("Tech One")
        assertThat(state.username).isEqualTo("tech1")
        assertThat(state.initials).isEqualTo("TO")
        assertThat(state.unsyncedCount).isEqualTo(1)
    }

    @Test
    fun loggingOutCancelsSyncAndEndsTheSession() = runTest {
        viewModel.logOut()

        assertThat(sessionStore.session.first { it == null }).isNull()
        assertThat(syncScheduler.cancelled).isTrue()
    }

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
