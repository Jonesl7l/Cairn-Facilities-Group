package com.jonesl7l.carinfg.feature.incident.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonesl7l.carinfg.core.data.incident.IncidentRepository
import com.jonesl7l.carinfg.core.data.preferences.UserPreferencesStore
import com.jonesl7l.carinfg.core.data.session.SessionStore
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSort
import com.jonesl7l.carinfg.core.domain.model.user.initials
import com.jonesl7l.carinfg.core.sync.SyncScheduler
import com.jonesl7l.carinfg.core.sync.status.SyncStatusProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class IncidentListViewModel @Inject constructor(
    incidentRepository: IncidentRepository,
    sessionStore: SessionStore,
    private val userPreferencesStore: UserPreferencesStore,
    private val syncScheduler: SyncScheduler,
    syncStatusProvider: SyncStatusProvider,
) : ViewModel() {
    private val scope = MutableStateFlow(IncidentScope.MINE)
    private val statusFilter = MutableStateFlow(StatusFilter.ALL)

    val uiState: StateFlow<IncidentListUiState> =
        combine(
            incidentRepository.observeIncidentList(),
            sessionStore.session,
            userPreferencesStore.incidentSort,
            scope,
            statusFilter,
        ) { items, user, sort, scope, statusFilter ->
            IncidentListUiState(
                isLoading = false,
                userInitials = user?.initials().orEmpty(),
                userName = user?.displayName.orEmpty(),
                scope = scope,
                statusFilter = statusFilter,
                sort = sort,
                sections =
                    IncidentListHelper.sections(
                        items = items,
                        userId = user?.id.orEmpty(),
                        scope = scope,
                        statusFilter = statusFilter,
                        sort = sort,
                    ),
            )
        }.combine(syncStatusProvider.status) { state, syncStatus -> state.copy(syncStatus = syncStatus) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeout = stopTimeout),
                initialValue = IncidentListUiState(),
            )

    fun onScopeChange(scope: IncidentScope) {
        this.scope.value = scope
    }

    fun onStatusFilterChange(statusFilter: StatusFilter) {
        this.statusFilter.value = statusFilter
    }

    fun onSortChange(sort: IncidentSort) {
        viewModelScope.launch { userPreferencesStore.setIncidentSort(sort = sort) }
    }

    fun onRefresh() {
        syncScheduler.requestSync()
    }

    fun onSyncStatusClick() {
        if (uiState.value.isRefreshing) return
        syncScheduler.requestSync()
    }

    private companion object {
        val stopTimeout = 5.seconds
    }
}
