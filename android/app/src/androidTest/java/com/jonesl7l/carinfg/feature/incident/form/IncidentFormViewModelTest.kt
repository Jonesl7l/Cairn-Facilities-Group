package com.jonesl7l.carinfg.feature.incident.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.MainDispatcherRule
import com.jonesl7l.carinfg.core.data.incident.IncidentRepositoryImpl
import com.jonesl7l.carinfg.core.data.reference.ReferenceRepository
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.EntityTestHelper.INCIDENT_ID
import com.jonesl7l.carinfg.core.database.EntityTestHelper.buildingEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.referenceEntities
import com.jonesl7l.carinfg.core.database.EntityTestHelper.siteEntity
import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSafety
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentType
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.datasource.FakeIncidentRemoteDataSource
import com.jonesl7l.carinfg.core.network.datasource.FakeReferenceRemoteDataSource
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import com.jonesl7l.carinfg.core.sync.FakeSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncidentFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val database: AppDatabase = inMemoryDatabase()
    private val incidentRepository =
        IncidentRepositoryImpl(
            database = database,
            incidentDao = database.incidentDao(),
            referenceDao = database.referenceDao(),
            sessionStore = FakeSessionStore(initial = technician),
            syncScheduler = FakeSyncScheduler(),
            incidentRemoteDataSource = FakeIncidentRemoteDataSource(),
        )
    private val referenceRepository =
        ReferenceRepository(
            referenceDao = database.referenceDao(),
            referenceRemoteDataSource = FakeReferenceRemoteDataSource(result = ApiResult.Failure(error = ApiError.NotFound)),
            applicationScope = CoroutineScope(context = Job()),
        )
    private val viewModels = mutableListOf<IncidentFormViewModel>()

    @Before
    fun setUp() = runTest {
        database.referenceDao().insertAllIfEmpty(
            entities = referenceEntities(
                sites = listOf(
                    siteEntity(),
                    siteEntity(
                        id = "site-2",
                        name = "South Depot",
                    ),
                ),
                buildings = listOf(
                    buildingEntity(),
                    buildingEntity(
                        id = "building-2",
                        siteId = "site-2",
                        name = "Workshop",
                    ),
                ),
            ),
        )
    }

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        database.close()
    }

    @Test
    fun savingADraftStoresTheChangedForm() = runTest {
        val viewModel = loadedViewModel()
        viewModel.saveDraft()
        advanceUntilIdle()
        assertThat(incidentRepository.observeIncidentList().first()).isEmpty()

        viewModel.update { it.copy(details = it.details.copy(title = "Leak")) }
        viewModel.saveDraft()

        val drafts = incidentRepository.observeIncidentList().first { it.isNotEmpty() }
        assertThat(drafts.single().title).isEqualTo("Leak")
        assertThat(drafts.single().syncState).isEqualTo(SyncState.DRAFT)
    }

    @Test
    fun aDraftReopensAfterProcessDeath() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = loadedViewModel(savedStateHandle = savedStateHandle)
        viewModel.update { it.copy(details = it.details.copy(title = "Leak")) }
        viewModel.saveDraft()
        incidentRepository.observeIncidentList().first { it.isNotEmpty() }

        val restored = loadedViewModel(savedStateHandle = savedStateHandle)

        assertThat(restored.uiState.value.fields?.details?.title).isEqualTo("Leak")
        assertThat(restored.uiState.value.isDraft).isTrue()
    }

    @Test
    fun anIncompleteReportShowsItsErrorsOnlyAfterSubmit() = runTest {
        val viewModel = loadedViewModel()
        assertThat(viewModel.uiState.value.errors).isEmpty()
        assertThat(viewModel.uiState.value.remainingErrors).isNotEmpty()

        viewModel.submit()

        val state = viewModel.uiState.first { it.outcome != null }
        assertThat(state.outcome).isEqualTo(FormOutcome.INVALID)
        assertThat(state.errors).isNotEmpty()
    }

    @Test
    fun aCompleteReportIsQueuedForSync() = runTest {
        val viewModel = loadedViewModel()
        viewModel.complete()

        viewModel.submit()

        assertThat(viewModel.uiState.first { it.outcome != null }.outcome).isEqualTo(FormOutcome.SAVED)
        val queued = incidentRepository.observeIncidentList().first { list -> list.any { it.syncState == SyncState.PENDING_CREATE } }
        assertThat(queued.single().title).isEqualTo("Leaking pipe")
    }

    @Test
    fun anEditIsOnlyStoredOnSave() = runTest {
        database.incidentDao().upsertIncident(incident = incidentEntity(reporterId = technician.id))
        val viewModel = loadedViewModel(incidentId = INCIDENT_ID)

        viewModel.update { it.copy(details = it.details.copy(title = "Flooded lobby")) }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isDraft).isFalse()
        assertThat(viewModel.uiState.value.hasChanges).isTrue()
        assertThat(storedTitle()).isEqualTo("Wet floor in lobby")
        viewModel.submit()
        assertThat(viewModel.uiState.first { it.outcome != null }.outcome).isEqualTo(FormOutcome.SAVED)
        assertThat(storedTitle()).isEqualTo("Flooded lobby")
    }

    @Test
    fun aValidationProblemShowsItsErrorsStraightAway() = runTest {
        database.incidentDao().upsertIncident(
            incident = incidentEntity(
                reporterId = technician.id,
                title = "",
                syncState = SyncState.NEEDS_ATTENTION,
                conflictReason = ConflictReason.VALIDATION,
            ),
        )

        val viewModel = loadedViewModel(incidentId = INCIDENT_ID)

        assertThat(viewModel.uiState.value.errors).isNotEmpty()
    }

    @Test
    fun closingAnUntouchedFormLeavesNoDraft() = runTest {
        val viewModel = loadedViewModel()

        viewModel.closeDraft()

        assertThat(viewModel.uiState.first { it.outcome != null }.outcome).isEqualTo(FormOutcome.CLOSED)
        assertThat(incidentRepository.observeIncidentList().first()).isEmpty()
    }

    @Test
    fun closingAChangedFormSavesTheDraft() = runTest {
        val viewModel = loadedViewModel()
        viewModel.update { it.copy(details = it.details.copy(title = "Leak")) }

        viewModel.closeDraft()

        assertThat(viewModel.uiState.first { it.outcome != null }.outcome).isEqualTo(FormOutcome.DRAFT_SAVED)
        assertThat(incidentRepository.observeIncidentList().first()).hasSize(1)
    }

    @Test
    fun choosingASiteListsItsBuildings() = runTest {
        val viewModel = loadedViewModel()

        viewModel.update { it.copy(location = it.location.copy(siteId = "site-2")) }

        val buildings = viewModel.uiState.value.options.buildings
        assertThat(buildings.map { it.id }).isEqualTo(listOf("building-2"))
    }

    @Test
    fun anotherPersonNeedsTheLastOneFilledIn() = runTest {
        val viewModel = loadedViewModel()

        viewModel.addPerson()
        viewModel.addPerson()

        assertThat(viewModel.uiState.value.fields?.persons.orEmpty()).hasSize(1)
        assertThat(viewModel.uiState.value.canAddPerson).isFalse()
    }

    private suspend fun loadedViewModel(
        incidentId: String? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): IncidentFormViewModel {
        val viewModel =
            IncidentFormViewModel(
                incidentId = incidentId,
                savedStateHandle = savedStateHandle,
                incidentRepository = incidentRepository,
                referenceRepository = referenceRepository,
                sessionStore = FakeSessionStore(initial = technician),
            )
        viewModels += viewModel
        viewModel.uiState.first { !it.isLoading && it.options.sites.isNotEmpty() }
        return viewModel
    }

    private fun IncidentFormViewModel.complete() {
        update {
            it.copy(
                details = it.details.copy(
                    title = "Leaking pipe",
                    description = "Water under the sink",
                    type = IncidentType.WaterLeak,
                    severity = Severity.Medium,
                ),
                location = it.location.copy(siteId = "site-1"),
                safety = IncidentSafety(
                    injuryOccurred = false,
                    hazardStillPresent = false,
                    immediateActions = setOf(ImmediateAction.None),
                ),
            )
        }
        update { it.copy(location = it.location.copy(buildingId = "building-1")) }
    }

    private suspend fun storedTitle(): String? = database.incidentDao().getIncident(id = INCIDENT_ID)?.incident?.title

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
