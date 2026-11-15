package com.jonesl7l.carinfg.core.sync

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.database.EntityTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.ApiTestHelper.incidentDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.now
import com.jonesl7l.carinfg.core.network.datasource.FakeIncidentRemoteDataSource
import com.jonesl7l.carinfg.core.network.datasource.IncidentChangesPage
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import com.jonesl7l.carinfg.core.sync.pull.PullEngine
import com.jonesl7l.carinfg.core.sync.push.PushEngine
import com.jonesl7l.carinfg.core.sync.store.FakeSyncLocalStore
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncRunnerTest {
    private val remote = FakeIncidentRemoteDataSource()
    private val store = FakeSyncLocalStore()
    private val sessionStore = FakeSessionStore(initial = technician)
    private val runner =
        SyncRunner(
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

    @Test
    fun `a run without a session does nothing`() = runTest {
        sessionStore.clear()

        assertThat(runner.run(clock = clock)).isEqualTo(SyncRunResult.Done)
        assertThat(remote.changeRequests).isEmpty()
    }

    @Test
    fun `a run is skipped while the session is expired`() = runTest {
        sessionStore.markSessionExpired()

        assertThat(runner.run(clock = clock)).isEqualTo(SyncRunResult.SessionExpired)
        assertThat(remote.changeRequests).isEmpty()
    }

    @Test
    fun `a run pushes then pulls once when nothing is left`() = runTest {
        pending()
        remote.createResults += ApiResult.Success(value = incidentDto(photos = emptyList()))
        remote.changePages += lastPage()

        val result = runner.run(clock = clock)

        assertThat(result).isEqualTo(SyncRunResult.Done)
        assertThat(remote.createRequests.size).isEqualTo(1)
        assertThat(remote.changeRequests.size).isEqualTo(1)
    }

    @Test
    fun `a run pulls again while an announced change has not arrived`() = runTest {
        store.meta = store.meta.copy(announcedChangeSeq = 5)
        remote.changePages += lastPage()
        remote.changePages += lastPage(incidents = listOf(incidentDto(changeSeq = 5)))

        val result = runner.run(clock = clock)

        assertThat(result).isEqualTo(SyncRunResult.Done)
        assertThat(remote.changeRequests.size).isEqualTo(2)
        assertThat(store.meta.lastChangeSeq).isEqualTo(5L)
    }

    @Test
    fun `a run stops after three passes when a row stays pending`() = runTest {
        pending(syncState = SyncState.PENDING_UPDATE)
        repeat(times = 4) {
            remote.updateResults += ApiResult.Failure(error = ApiError.Forbidden)
            remote.changePages += lastPage()
        }

        val result = runner.run(clock = clock)

        assertThat(result).isEqualTo(SyncRunResult.Done)
        assertThat(remote.updateVersions.size).isEqualTo(3)
    }

    @Test
    fun `a 401 from push marks the session expired and skips the pull`() = runTest {
        pending(syncState = SyncState.PENDING_UPDATE)
        remote.updateResults += ApiResult.Failure(error = ApiError.Unauthenticated)

        val result = runner.run(clock = clock)

        assertThat(result).isEqualTo(SyncRunResult.SessionExpired)
        assertThat(sessionStore.isSessionExpired.first()).isTrue()
        assertThat(remote.changeRequests).isEmpty()
    }

    @Test
    fun `a 401 from pull marks the session expired`() = runTest {
        remote.changePages += ApiResult.Failure(error = ApiError.Unauthenticated)

        assertThat(runner.run(clock = clock)).isEqualTo(SyncRunResult.SessionExpired)
        assertThat(sessionStore.isSessionExpired.first()).isTrue()
    }

    @Test
    fun `a network error from push retries with backoff`() = runTest {
        pending(syncState = SyncState.PENDING_UPDATE)
        remote.updateResults += ApiResult.Failure(error = ApiError.Network(cause = IOException()))

        assertThat(runner.run(clock = clock)).isEqualTo(SyncRunResult.RetryWithBackoff)
        assertThat(sessionStore.isSessionExpired.first()).isFalse()
    }

    @Test
    fun `a 503 from pull retries after its delay`() = runTest {
        remote.changePages += ApiResult.Failure(error = ApiError.ServiceUnavailable(retryAfter = 30.seconds))

        assertThat(runner.run(clock = clock)).isEqualTo(SyncRunResult.RetryAfter(delay = 30.seconds))
    }

    @Test
    fun `a 503 from pull without a delay retries with backoff`() = runTest {
        remote.changePages += ApiResult.Failure(error = ApiError.ServiceUnavailable(retryAfter = null))

        assertThat(runner.run(clock = clock)).isEqualTo(SyncRunResult.RetryWithBackoff)
    }

    @Test
    fun `a pull error that points to an app bug ends the run`() = runTest {
        remote.changePages += ApiResult.Failure(error = ApiError.MalformedRequest)

        assertThat(runner.run(clock = clock)).isEqualTo(SyncRunResult.Done)
        assertThat(remote.changeRequests.size).isEqualTo(1)
    }

    private fun pending(syncState: SyncState = SyncState.PENDING_CREATE) {
        val row = incidentEntity(syncState = syncState)
        store.incidents[row.id] = IncidentWithPhotos(incident = row, photos = emptyList())
    }

    private fun lastPage(incidents: List<IncidentDto> = emptyList()) = ApiResult.Success(
        value =
            IncidentChangesPage(
                incidents = incidents,
                databaseId = "db-1",
                isLastPage = true,
            ),
    )

    private companion object {
        val technician =
            User(
                id = "u1",
                username = "tech1",
                displayName = "Tech One",
                role = UserRole.Technician,
            )

        val clock =
            object : Clock {
                override fun now(): Instant = now
            }
    }
}
