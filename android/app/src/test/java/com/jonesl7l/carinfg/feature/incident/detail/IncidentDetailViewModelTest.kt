package com.jonesl7l.carinfg.feature.incident.detail

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.MainDispatcherRule
import com.jonesl7l.carinfg.core.data.incident.DeleteResult
import com.jonesl7l.carinfg.core.data.incident.FakeIncidentRepository
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.REPORTER_ID
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incident
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.operatorFields
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.syncStatus
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentDetail
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentNames
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class IncidentDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeIncidentRepository()
    private val viewModel by lazy {
        IncidentDetailViewModel(
            incidentId = INCIDENT_ID,
            incidentRepository = repository,
            sessionStore = FakeSessionStore(initial = reporter),
        )
    }

    @Test
    fun `my open incident can be edited and deleted`() = runTest {
        show(incident = incident())

        val state = latest()

        assertThat(state.isMine).isTrue()
        assertThat(state.canEdit).isTrue()
        assertThat(state.canDelete).isTrue()
        assertThat(state.isLocked).isFalse()
    }

    @Test
    fun `another technicians incident is view only`() = runTest {
        show(incident = incident(reporterId = "tech2"))

        val state = latest()

        assertThat(state.isMine).isFalse()
        assertThat(state.canEdit).isFalse()
        assertThat(state.canDelete).isFalse()
        assertThat(state.isLocked).isFalse()
    }

    @Test
    fun `my incident in progress is locked`() = runTest {
        show(incident = incident(operatorFields = operatorFields(status = IncidentStatus.InProgress)))

        val state = latest()

        assertThat(state.canEdit).isFalse()
        assertThat(state.canDelete).isFalse()
        assertThat(state.isLocked).isTrue()
    }

    @Test
    fun `an incident needing attention cannot be edited or deleted and is not shown as locked`() = runTest {
        show(
            incident =
                incident(
                    syncStatus =
                        syncStatus(
                            state = SyncState.NEEDS_ATTENTION,
                            conflictReason = ConflictReason.DELETED_REMOTELY,
                        ),
                ),
        )

        val state = latest()

        assertThat(state.needsAttention).isTrue()
        assertThat(state.canEdit).isFalse()
        assertThat(state.canDelete).isFalse()
        assertThat(state.isLocked).isFalse()
    }

    @Test
    fun `a missing incident stops loading with no detail`() = runTest {
        val state = latest()

        assertThat(state.detail).isNull()
    }

    @Test
    fun `deleting a synced incident can be undone`() = runTest {
        show(incident = incident())
        repository.deleteResult = DeleteResult.PendingDelete(undoDeadline = Instant.DISTANT_FUTURE)
        latest()

        viewModel.delete()

        assertThat(repository.deletedIds).containsExactly(INCIDENT_ID)
        assertThat(latest().deleteOutcome).isEqualTo(DeleteOutcome.UNDOABLE)
    }

    @Test
    fun `deleting a draft removes it with no undo`() = runTest {
        show(incident = incident())
        repository.deleteResult = DeleteResult.Removed
        latest()

        viewModel.delete()

        assertThat(latest().deleteOutcome).isEqualTo(DeleteOutcome.REMOVED)
    }

    private fun show(incident: Incident) {
        repository.incidentDetail.value =
            IncidentDetail(
                incident = incident,
                names = IncidentNames(),
            )
    }

    private suspend fun TestScope.latest(): IncidentDetailUiState {
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel.uiState.first { !it.isLoading }
    }

    private companion object {
        const val INCIDENT_ID = "incident-1"
        val reporter =
            User(
                id = REPORTER_ID,
                username = REPORTER_ID,
                displayName = "Tech One",
                role = UserRole.Technician,
            )
    }
}
