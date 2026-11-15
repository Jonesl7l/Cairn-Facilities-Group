package com.jonesl7l.carinfg.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonesl7l.carinfg.core.data.session.SessionRepository
import com.jonesl7l.carinfg.core.domain.model.user.initials
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
class AccountViewModel @Inject constructor(private val sessionRepository: SessionRepository) : ViewModel() {
    private val isLoggingOut = MutableStateFlow(false)

    val uiState: StateFlow<AccountUiState> =
        combine(
            sessionRepository.observeUser(),
            sessionRepository.observeUnsyncedCount(),
            isLoggingOut,
        ) { user, unsyncedCount, isLoggingOut ->
            AccountUiState(
                displayName = user?.displayName.orEmpty(),
                username = user?.username.orEmpty(),
                initials = user?.initials().orEmpty(),
                unsyncedCount = unsyncedCount,
                isLoggingOut = isLoggingOut,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeout = stopTimeout),
            initialValue = AccountUiState(),
        )

    fun logOut() {
        if (isLoggingOut.value) return
        isLoggingOut.value = true
        viewModelScope.launch { sessionRepository.logout() }
    }

    private companion object {
        val stopTimeout = 5.seconds
    }
}
