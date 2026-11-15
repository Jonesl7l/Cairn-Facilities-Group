package com.jonesl7l.carinfg.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonesl7l.carinfg.core.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AppViewModel @Inject constructor(sessionStore: SessionStore) : ViewModel() {
    val uiState: StateFlow<AppUiState> =
        combine(
            sessionStore.session,
            sessionStore.isSessionExpired,
        ) { user, isSessionExpired ->
            if (user == null) {
                AppUiState.LoggedOut
            } else {
                AppUiState.LoggedIn(
                    user = user,
                    isSessionExpired = isSessionExpired,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeout = stopTimeout),
            initialValue = AppUiState.Loading,
        )

    private companion object {
        val stopTimeout = 5.seconds
    }
}
