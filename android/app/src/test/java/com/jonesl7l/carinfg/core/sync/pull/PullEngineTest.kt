package com.jonesl7l.carinfg.core.sync.pull

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.ApiTestHelper.REPORTER_ID
import com.jonesl7l.carinfg.core.network.ApiTestHelper.incidentDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.now
import com.jonesl7l.carinfg.core.network.datasource.FakeIncidentRemoteDataSource
import com.jonesl7l.carinfg.core.network.datasource.IncidentChangesPage
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import com.jonesl7l.carinfg.core.sync.SyncRunResult
import com.jonesl7l.carinfg.core.sync.store.FakeSyncLocalStore
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PullEngineTest {
    private val remote = FakeIncidentRemoteDataSource()
    private val store = FakeSyncLocalStore()
    private val sessionStore = FakeSessionStore(initial = technician)
    private val engine =
        PullEngine(
            incidentRemoteDataSource = remote,
            syncLocalStore = store,
            sessionStore = sessionStore,
        )

    @Test
    fun `pages are pulled until a short page and the cursor follows the last change`() = runTest {
        remote.changePages += page(incidents = listOf(incident(id = "a", changeSeq = 10)), isLastPage = false)
        remote.changePages += page(incidents = listOf(incident(id = "b", changeSeq = 11)), isLastPage = true)

        val result = engine.pull(now = now)

        assertThat(result).isEqualTo(SyncRunResult.Done)
        assertThat(remote.changeRequests).containsExactly(0L, 10L)
        assertThat(store.meta.lastChangeSeq).isEqualTo(11L)
        assertThat(store.incidents.keys.toList()).containsExactly("a", "b")
    }

    @Test
    fun `the first pull stores the database id`() = runTest {
        remote.changePages += page(incidents = emptyList(), databaseId = "db-1")

        engine.pull(now = now)

        assertThat(store.meta.databaseId).isEqualTo("db-1")
    }

    @Test
    fun `a changed database id resets and pulls again from the start`() = runTest {
        store.meta = store.meta.copy(lastChangeSeq = 90, databaseId = "db-1")
        remote.changePages += page(incidents = emptyList(), databaseId = "db-2")
        remote.changePages += page(incidents = listOf(incident(id = "a", changeSeq = 1)), databaseId = "db-2")

        engine.pull(now = now)

        assertThat(store.resets).containsExactly("db-2")
        assertThat(remote.changeRequests).containsExactly(90L, 0L)
        assertThat(store.meta.lastChangeSeq).isEqualTo(1L)
    }

    @Test
    fun `a failed page stops the pull and keeps the cursor`() = runTest {
        store.meta = store.meta.copy(lastChangeSeq = 5, databaseId = "db-1")
        val error = ApiError.ServiceUnavailable(retryAfter = null)
        remote.changePages += ApiResult.Failure(error = error)

        assertThat(engine.pull(now = now)).isEqualTo(SyncRunResult.RetryWithBackoff)
        assertThat(store.meta.lastChangeSeq).isEqualTo(5L)
        assertThat(store.prunes).isEmpty()
    }

    @Test
    fun `without a session nothing is pulled`() = runTest {
        sessionStore.clear()

        assertThat(engine.pull(now = now)).isEqualTo(SyncRunResult.Done)
        assertThat(remote.changeRequests).isEmpty()
    }

    @Test
    fun `out of scope rows are pruned after the last page`() = runTest {
        remote.changePages += page(incidents = emptyList())

        engine.pull(now = now)

        assertThat(store.prunes).containsExactly(REPORTER_ID to now - PullRuleHelper.scopeWindow)
    }

    private fun incident(
        id: String,
        changeSeq: Long,
    ): IncidentDto = incidentDto(
        id = id,
        changeSeq = changeSeq,
    )

    private fun page(
        incidents: List<IncidentDto>,
        databaseId: String = "db-1",
        isLastPage: Boolean = true,
    ): ApiResult<IncidentChangesPage> = ApiResult.Success(
        value =
            IncidentChangesPage(
                incidents = incidents,
                databaseId = databaseId,
                isLastPage = isLastPage,
            ),
    )

    private companion object {
        val technician =
            User(
                id = REPORTER_ID,
                username = REPORTER_ID,
                displayName = "Tech One",
                role = UserRole.Technician,
            )
    }
}
