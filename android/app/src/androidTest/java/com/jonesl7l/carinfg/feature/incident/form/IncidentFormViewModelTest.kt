package com.jonesl7l.carinfg.feature.incident.form

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.MainDispatcherRule
import com.jonesl7l.carinfg.core.data.incident.IncidentRepositoryImpl
import com.jonesl7l.carinfg.core.data.photo.PhotoStorage
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
import com.jonesl7l.carinfg.core.domain.model.photo.PhotoUploadState
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.datasource.FakeIncidentRemoteDataSource
import com.jonesl7l.carinfg.core.network.datasource.FakeReferenceRemoteDataSource
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import com.jonesl7l.carinfg.core.sync.FakeSyncScheduler
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
    val mainDispatcherRule = MainDispatcherRule(dispatcher = StandardTestDispatcher())

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
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val photoStorage =
        PhotoStorage(
            context = context,
            incidentDao = database.incidentDao(),
            ioDispatcher = Dispatchers.IO,
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
        File(context.filesDir, "incidents").deleteRecursively()
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

    // region photos
    @Test
    fun pickedPhotosAreAddedAsPending() = runTest {
        val viewModel = loadedViewModel()

        viewModel.addPicked(uris = pictures(count = 2))

        val photos = viewModel.photosOnceThereAre(count = 2)
        assertThat(photos.map { it.uploadState }).containsExactly(
            PhotoUploadState.PENDING,
            PhotoUploadState.PENDING,
        )
        assertThat(photos.all { File(it.localPath.orEmpty()).exists() }).isTrue()
        assertThat(viewModel.uiState.value.photosLeft).isEqualTo(3)
    }

    @Test
    fun pickingMoreThanFitKeepsOnlyTheRoomLeft() = runTest {
        val viewModel = loadedViewModel()

        viewModel.addPicked(uris = pictures(count = 6))
        viewModel.photosOnceThereAre(count = 5)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.fields?.photos.orEmpty()).hasSize(5)
        assertThat(viewModel.uiState.value.photosLeft).isEqualTo(0)
    }

    @Test
    fun aPhotoCanBeMadeTheCoverOrRemoved() = runTest {
        val viewModel = loadedViewModel()
        viewModel.addPicked(uris = pictures(count = 2))
        val (first, second) = viewModel.photosOnceThereAre(count = 2)

        viewModel.makeCover(index = 1)
        assertThat(viewModel.uiState.value.fields?.photos.orEmpty().map { it.id }).containsExactly(
            second.id,
            first.id,
        )

        viewModel.removePhoto(index = 0)
        assertThat(viewModel.uiState.value.fields?.photos.orEmpty().map { it.id }).containsExactly(first.id)
    }

    @Test
    fun aPhotoTakenWhileTheProcessDiedIsAdded() = runTest {
        val savedStateHandle = SavedStateHandle()
        loadedViewModel(savedStateHandle = savedStateHandle).cameraTarget()
        val photoId = checkNotNull(savedStateHandle.get<String>("cameraPhotoId"))
        val incidentId = checkNotNull(savedStateHandle.get<String>("incidentId"))
        picture(
            file = photoStorage.photoFile(
                incidentId = incidentId,
                photoId = photoId,
            ),
        )
        val restored = loadedViewModel(savedStateHandle = savedStateHandle)

        restored.onPhotoTaken(success = true)

        assertThat(restored.photosOnceThereAre(count = 1).single().id).isEqualTo(photoId)
    }

    @Test
    fun aPhotoThatCannotBeReadIsReported() = runTest {
        val viewModel = loadedViewModel()

        viewModel.addPicked(uris = listOf(Uri.fromFile(File(context.cacheDir, "missing.jpg"))))

        assertThat(viewModel.uiState.first { it.outcome != null }.outcome).isEqualTo(FormOutcome.PHOTO_NOT_SAVED)
        assertThat(viewModel.uiState.value.fields?.photos.orEmpty()).isEmpty()
    }
    // endregion

    private suspend fun IncidentFormViewModel.photosOnceThereAre(count: Int) =
        uiState.first { it.fields?.photos?.size == count }.fields?.photos.orEmpty()

    private fun pictures(count: Int): List<Uri> = (1..count).map {
        Uri.fromFile(picture(file = File(context.cacheDir, "picked-$it.jpg")))
    }

    private fun picture(file: File): File {
        val bitmap = Bitmap.createBitmap(
            PICTURE_SIZE,
            PICTURE_SIZE,
            Bitmap.Config.ARGB_8888,
        )
        file.outputStream().use {
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                PICTURE_QUALITY,
                it,
            )
        }
        return file
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
                photoStorage = photoStorage,
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
        const val PICTURE_SIZE = 100
        const val PICTURE_QUALITY = 80
        val technician =
            User(
                id = "u1",
                username = "tech1",
                displayName = "Tech One",
                role = UserRole.Technician,
            )
    }
}
