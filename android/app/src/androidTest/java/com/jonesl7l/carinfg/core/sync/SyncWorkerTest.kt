package com.jonesl7l.carinfg.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.datasource.FakeIncidentRemoteDataSource
import com.jonesl7l.carinfg.core.network.datasource.IncidentChangesPage
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import com.jonesl7l.carinfg.core.sync.pull.PullEngine
import com.jonesl7l.carinfg.core.sync.push.PushEngine
import com.jonesl7l.carinfg.core.sync.store.RoomSyncLocalStore
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database: AppDatabase = inMemoryDatabase()
    private val remote = FakeIncidentRemoteDataSource()
    private val sessionStore = FakeSessionStore(initial = technician)
    private val syncScheduler = FakeSyncScheduler()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aCompleteRunSucceeds() = runTest {
        remote.changePages += lastPage()

        assertThat(worker().doWork()).isEqualTo(ListenableWorker.Result.success())
        assertThat(syncScheduler.delayedRequests).isEmpty()
    }

    @Test
    fun aNetworkErrorRetriesWithBackoff() = runTest {
        remote.changePages += ApiResult.Failure(error = ApiError.Network(cause = IOException()))

        assertThat(worker().doWork()).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun aRetryAfterSucceedsAndAsksForADelayedSync() = runTest {
        remote.changePages += ApiResult.Failure(error = ApiError.ServiceUnavailable(retryAfter = 30.seconds))

        assertThat(worker().doWork()).isEqualTo(ListenableWorker.Result.success())
        assertThat(syncScheduler.delayedRequests).containsExactly(30.seconds)
    }

    @Test
    fun anExpiredSessionSucceedsWithoutRetrying() = runTest {
        remote.changePages += ApiResult.Failure(error = ApiError.Unauthenticated)

        assertThat(worker().doWork()).isEqualTo(ListenableWorker.Result.success())
    }

    private fun worker(): SyncWorker = TestListenableWorkerBuilder<SyncWorker>(context)
        .setWorkerFactory(SyncWorkerFactory(syncRunner = syncRunner(), syncScheduler = syncScheduler))
        .build()

    private fun syncRunner(): SyncRunner {
        val store =
            RoomSyncLocalStore(
                database = database,
                incidentDao = database.incidentDao(),
                syncMetaDao = database.syncMetaDao(),
            )
        return SyncRunner(
            pushEngine =
                PushEngine(
                    incidentRemoteDataSource = remote,
                    syncLocalStore = store,
                    sessionStore = sessionStore,
                ),
            pullEngine =
                PullEngine(
                    incidentRemoteDataSource = remote,
                    syncLocalStore = store,
                    sessionStore = sessionStore,
                ),
            syncLocalStore = store,
            sessionStore = sessionStore,
        )
    }

    private fun lastPage() = ApiResult.Success(
        value =
            IncidentChangesPage(
                incidents = emptyList(),
                databaseId = "db-1",
                isLastPage = true,
            ),
    )

    private class SyncWorkerFactory(
        private val syncRunner: SyncRunner,
        private val syncScheduler: SyncScheduler,
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker = SyncWorker(
            context = appContext,
            params = workerParameters,
            syncRunner = syncRunner,
            syncScheduler = syncScheduler,
        )
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
