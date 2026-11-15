package com.jonesl7l.carinfg.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonesl7l.carinfg.core.data.session.LoginResult
import com.jonesl7l.carinfg.core.data.session.SessionRepository
import com.jonesl7l.carinfg.core.network.result.ApiError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(private val sessionRepository: SessionRepository) : ViewModel() {
    private val state = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = state.asStateFlow()

    init {
        viewModelScope.launch { lockToExpiredSession() }
    }

    fun onUsernameChange(username: String) {
        if (state.value.isUsernameLocked) return
        state.update {
            it.copy(
                username = username,
                error = null,
            )
        }
    }

    fun onPasswordChange(password: String) {
        state.update {
            it.copy(
                password = password,
                error = null,
            )
        }
    }

    fun logIn() {
        val current = state.value
        if (!current.canLogIn) return
        state.update {
            it.copy(
                isLoggingIn = true,
                error = null,
            )
        }
        viewModelScope.launch {
            val result =
                sessionRepository.login(
                    username = current.username.trim(),
                    password = current.password,
                )
            val error = result.toError()
            state.update {
                it.copy(
                    isLoggingIn = error == null,
                    error = error,
                )
            }
        }
    }

    private suspend fun lockToExpiredSession() {
        val user = sessionRepository.observeUser().first() ?: return
        state.update {
            it.copy(
                username = user.username,
                isUsernameLocked = true,
            )
        }
    }

    private fun LoginResult.toError(): LoginError? = when (this) {
        is LoginResult.Success -> null
        LoginResult.InvalidCredentials -> LoginError.INVALID_CREDENTIALS
        LoginResult.NotTechnician -> LoginError.NOT_TECHNICIAN
        is LoginResult.Failed -> if (error is ApiError.Network) LoginError.OFFLINE else LoginError.FAILED
    }
}
