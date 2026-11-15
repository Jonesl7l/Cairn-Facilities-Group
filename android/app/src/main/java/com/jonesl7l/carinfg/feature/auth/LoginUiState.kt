package com.jonesl7l.carinfg.feature.auth

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isUsernameLocked: Boolean = false,
    val isLoggingIn: Boolean = false,
    val error: LoginError? = null,
) {
    val canLogIn: Boolean
        get() = username.isNotBlank() && password.isNotEmpty() && !isLoggingIn
}
