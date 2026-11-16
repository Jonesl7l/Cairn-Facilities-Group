package com.jonesl7l.carinfg.feature.incident.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonesl7l.carinfg.core.data.incident.DeleteResult
import com.jonesl7l.carinfg.core.data.incident.IncidentRepository
import com.jonesl7l.carinfg.core.data.incident.ResolveResult
import com.jonesl7l.carinfg.core.data.session.SessionStore
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.policy.IncidentPolicyHelper
import com.jonesl7l.carinfg.feature.incident.incidentUndoWindow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = IncidentDetailViewModel.Factory::class)
class IncidentDetailViewModel @AssistedInject constructor(
    @Assisted private val incidentId: String,
    private val incidentRepository: IncidentRepository,
    sessionStore: SessionStore,
) : ViewModel() {
    private val deleteOutcome = MutableStateFlow<DeleteOutcome?>(null)
    private val isResolveFailed = MutableStateFlow(false)

    val uiState: StateFlow<IncidentDetailUiState> =
        combine(
            incidentRepository.observeIncidentDetail(id = incidentId),
            sessionStore.session,
            deleteOutcome,
            isResolveFailed,
        ) { detail, user, deleteOutcome, isResolveFailed ->
            val incident = detail?.incident
            val userId = user?.id
            val isEditable = incident != null && userId != null && incident.syncStatus.state != SyncState.NEEDS_ATTENTION
            IncidentDetailUiState(
                isLoading = false,
                detail = detail,
                isMine = incident != null && incident.reporterId == userId,
                canEdit =
                    isEditable &&
                        IncidentPolicyHelper.canEdit(
                            incident = incident,
                            userId = userId,
                        ),
                canDelete =
                    isEditable &&
                        IncidentPolicyHelper.canDelete(
                            incident = incident,
                            userId = userId,
                        ),
                deleteOutcome = deleteOutcome,
                isResolveFailed = isResolveFailed,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeout = stopTimeout),
            initialValue = IncidentDetailUiState(),
        )

    fun delete() {
        viewModelScope.launch {
            val result =
                incidentRepository.delete(
                    id = incidentId,
                    now = Clock.System.now(),
                    undoWindow = incidentUndoWindow,
                )
            deleteOutcome.value =
                when (result) {
                    DeleteResult.Removed -> DeleteOutcome.REMOVED
                    is DeleteResult.PendingDelete -> DeleteOutcome.UNDOABLE
                    DeleteResult.NotAllowed -> null
                }
        }
    }

    fun resolve(resolution: Resolution) {
        viewModelScope.launch {
            val result =
                when (resolution) {
                    Resolution.DISCARD -> incidentRepository.discard(id = incidentId)

                    Resolution.SUBMIT_AS_NEW -> incidentRepository.submitAsNew(
                        id = incidentId,
                        now = Clock.System.now(),
                    )

                    Resolution.KEEP_INCIDENT -> incidentRepository.keepIncident(id = incidentId)

                    Resolution.DISCARD_MY_CHANGES -> incidentRepository.discardMyChanges(id = incidentId)

                    Resolution.FIX_IN_FORM -> return@launch
                }
            when (result) {
                ResolveResult.DONE -> if (resolution in removingResolutions) deleteOutcome.value = DeleteOutcome.REMOVED
                ResolveResult.FAILED -> isResolveFailed.value = true
                ResolveResult.NOT_ALLOWED -> Unit
            }
        }
    }

    fun consumeResolveFailure() {
        isResolveFailed.value = false
    }

    @AssistedFactory
    interface Factory {
        fun create(incidentId: String): IncidentDetailViewModel
    }

    private companion object {
        val stopTimeout = 5.seconds
        val removingResolutions =
            setOf(
                Resolution.DISCARD,
                Resolution.SUBMIT_AS_NEW,
            )
    }
}
