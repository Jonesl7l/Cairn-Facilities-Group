package com.jonesl7l.carinfg.app

import com.jonesl7l.carinfg.core.domain.model.user.User

sealed interface AppUiState {
    data object Loading : AppUiState

    data object LoggedOut : AppUiState

    data class LoggedIn(
        val user: User,
        val isSessionExpired: Boolean,
    ) : AppUiState
}
