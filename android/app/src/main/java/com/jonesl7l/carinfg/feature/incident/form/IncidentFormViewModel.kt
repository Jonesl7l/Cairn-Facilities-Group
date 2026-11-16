package com.jonesl7l.carinfg.feature.incident.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonesl7l.carinfg.core.data.incident.IncidentRepository
import com.jonesl7l.carinfg.core.data.incident.SaveResult
import com.jonesl7l.carinfg.core.data.reference.ReferenceRepository
import com.jonesl7l.carinfg.core.data.session.SessionStore
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.person.Person
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.validation.IncidentValidator
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

@HiltViewModel(assistedFactory = IncidentFormViewModel.Factory::class)
class IncidentFormViewModel @AssistedInject constructor(
    @Assisted incidentId: String?,
    savedStateHandle: SavedStateHandle,
    private val incidentRepository: IncidentRepository,
    private val referenceRepository: ReferenceRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val id: String = savedStateHandle[ID_KEY] ?: incidentId ?: UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(IncidentFormUiState())
    val uiState: StateFlow<IncidentFormUiState> = _uiState.asStateFlow()

    private var base: Incident? = null
    private var references = ReferenceOptions()
    private var hasSubmitted = false
    private var isDraftStored = false

    init {
        savedStateHandle[ID_KEY] = id
        viewModelScope.launch { load() }
        viewModelScope.launch { observeReferences() }
    }

    fun update(change: (IncidentFormFields) -> IncidentFormFields) {
        val fields = _uiState.value.fields ?: return
        render(
            fields = IncidentFormHelper.applyRules(
                previous = fields,
                next = change(fields),
            ),
        )
    }

    fun addPerson() {
        if (!_uiState.value.canAddPerson) return
        update { it.copy(persons = it.persons + Person(id = UUID.randomUUID().toString())) }
    }

    fun removePerson(index: Int) {
        update { it.copy(persons = it.persons.filterIndexed { personIndex, _ -> personIndex != index }) }
    }

    fun submit() {
        val base = base ?: return
        val fields = _uiState.value.fields ?: return
        hasSubmitted = true
        render(fields = fields)
        viewModelScope.launch {
            val incident = fields.applyTo(incident = base)
            val now = Clock.System.now()
            val result =
                if (base.isDraft()) {
                    incidentRepository.submit(
                        incident = incident,
                        now = now,
                    )
                } else {
                    incidentRepository.saveEdit(
                        incident = incident,
                        now = now,
                    )
                }
            val outcome =
                when (result) {
                    SaveResult.Saved -> FormOutcome.SAVED
                    is SaveResult.Invalid -> FormOutcome.INVALID
                    SaveResult.NotAllowed -> FormOutcome.NOT_ALLOWED
                }
            _uiState.update { it.copy(outcome = outcome) }
        }
    }

    fun saveDraft() {
        viewModelScope.launch { storeDraft() }
    }

    fun closeDraft() {
        viewModelScope.launch {
            storeDraft()
            val outcome = if (isDraftStored) FormOutcome.DRAFT_SAVED else FormOutcome.CLOSED
            _uiState.update { it.copy(outcome = outcome) }
        }
    }

    fun consumeOutcome() {
        _uiState.update { it.copy(outcome = null) }
    }

    private suspend fun load() {
        val stored = incidentRepository.observeIncident(id = id).first()
        isDraftStored = stored != null
        hasSubmitted = stored?.syncStatus?.state == SyncState.NEEDS_ATTENTION
        val incident =
            stored ?: IncidentFormHelper.newIncident(
                id = id,
                reporterId = sessionStore.session.first()?.id.orEmpty(),
                now = Clock.System.now(),
                timeZone = TimeZone.currentSystemDefault(),
            )
        base = incident
        render(fields = incident.toFormFields())
    }

    private suspend fun observeReferences() {
        combine(
            referenceRepository.observeSites(),
            referenceRepository.observeBuildings(),
            referenceRepository.observeFloors(),
            referenceRepository.observeRooms(),
            referenceRepository.observeAssets(),
        ) { sites, buildings, floors, rooms, assets ->
            ReferenceOptions(
                sites = sites,
                buildings = buildings,
                floors = floors,
                rooms = rooms,
                assets = assets,
            )
        }.collect { options ->
            references = options
            _uiState.value.fields?.let { render(fields = it) }
        }
    }

    private suspend fun storeDraft() {
        val base = base ?: return
        val fields = _uiState.value.fields ?: return
        if (!base.isDraft()) return
        if (!isDraftStored && fields == base.toFormFields()) return
        val result =
            incidentRepository.saveDraft(
                incident = fields.applyTo(incident = base),
                now = Clock.System.now(),
            )
        if (result == SaveResult.Saved) isDraftStored = true
    }

    private fun render(fields: IncidentFormFields) {
        val base = base ?: return
        val remainingErrors =
            IncidentValidator.validate(
                incident = fields.applyTo(incident = base),
                now = Clock.System.now(),
            )
        _uiState.update {
            it.copy(
                fields = fields,
                isDraft = base.isDraft(),
                options = IncidentFormHelper.optionsFor(
                    references = references,
                    location = fields.location,
                ),
                errors = if (hasSubmitted) remainingErrors else emptyList(),
                remainingErrors = remainingErrors,
                hasChanges = fields != base.toFormFields(),
                canAddPerson = IncidentFormHelper.canAddPerson(persons = fields.persons),
            )
        }
    }

    private fun Incident.isDraft() = syncStatus.state == SyncState.DRAFT

    @AssistedFactory
    interface Factory {
        fun create(incidentId: String?): IncidentFormViewModel
    }

    private companion object {
        const val ID_KEY = "incidentId"
    }
}
