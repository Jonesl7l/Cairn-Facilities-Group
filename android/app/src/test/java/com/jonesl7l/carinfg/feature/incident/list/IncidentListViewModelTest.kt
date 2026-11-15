package com.jonesl7l.carinfg.feature.incident.list

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.jonesl7l.carinfg.MainDispatcherRule
import com.jonesl7l.carinfg.core.data.incident.FakeIncidentRepository
import com.jonesl7l.carinfg.core.data.preferences.FakeUserPreferencesStore
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentListItem
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSort
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.sync.FakeSyncScheduler
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class IncidentListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeIncidentRepository()
    private val preferences = FakeUserPreferencesStore()
    private val scheduler = FakeSyncScheduler()
    private val viewModel by lazy {
        IncidentListViewModel(
            incidentRepository = repository,
            sessionStore = FakeSessionStore(initial = technician),
            userPreferencesStore = preferences,
            syncScheduler = scheduler,
        )
    }

    @Test
    fun `the list is built for the signed in user with their initials`() = runTest {
        repository.incidentList.value =
            listOf(
                item(id = "mine"),
                item(
                    id = "draft",
                    syncState = SyncState.DRAFT,
                ),
            )

        val state = latest()

        assertThat(state.isLoading).isFalse()
        assertThat(state.userInitials).isEqualTo("TO")
        assertThat(state.sections.incidents.map { it.id }).containsExactly("mine")
        assertThat(state.sections.drafts.map { it.id }).containsExactly("draft")
    }

    @Test
    fun `switching to team open shows other technicians open incidents`() = runTest {
        repository.incidentList.value =
            listOf(
                item(id = "mine"),
                item(
                    id = "theirs",
                    reporterId = "u2",
                ),
            )

        viewModel.onScopeChange(scope = IncidentScope.TEAM_OPEN)

        assertThat(latest().sections.incidents.map { it.id }).containsExactly("theirs")
    }

    @Test
    fun `a status filter narrows the list`() = runTest {
        repository.incidentList.value =
            listOf(
                item(id = "open"),
                item(
                    id = "assigned",
                    status = IncidentStatus.Assigned,
                ),
            )

        viewModel.onStatusFilterChange(statusFilter = StatusFilter.ASSIGNED)

        assertThat(latest().sections.incidents.map { it.id }).containsExactly("assigned")
    }

    @Test
    fun `choosing a sort saves it and re-sorts the list`() = runTest {
        repository.incidentList.value =
            listOf(
                item(
                    id = "low",
                    severity = Severity.Low,
                ),
                item(
                    id = "critical",
                    severity = Severity.Critical,
                    occurredAt = now - 1.hours,
                ),
            )

        viewModel.onSortChange(sort = IncidentSort.SEVERITY)

        assertThat(preferences.incidentSort.value).isEqualTo(IncidentSort.SEVERITY)
        assertThat(latest().sections.incidents.map { it.id }).containsExactly("critical", "low")
    }

    @Test
    fun `pulling to refresh asks for a sync`() {
        viewModel.onRefresh()

        assertThat(scheduler.syncRequests).isEqualTo(1)
    }

    private suspend fun latest(): IncidentListUiState = viewModel.uiState.first { !it.isLoading }

    private fun item(
        id: String,
        reporterId: String = technician.id,
        status: IncidentStatus = IncidentStatus.Open,
        severity: Severity = Severity.Medium,
        syncState: SyncState = SyncState.SYNCED,
        occurredAt: Instant = now,
    ) = IncidentListItem(
        id = id,
        title = "Incident $id",
        referenceNumber = null,
        reporterId = reporterId,
        status = status,
        severity = severity,
        syncState = syncState,
        syncAttempts = 0,
        occurredAt = occurredAt,
        localUpdatedAt = occurredAt,
        siteName = null,
        buildingName = null,
    )

    private companion object {
        val now: Instant = Instant.parse(input = "2026-11-15T12:00:00Z")
        val technician =
            User(
                id = "u1",
                username = "tech1",
                displayName = "Tech One",
                role = UserRole.Technician,
            )
    }
}
