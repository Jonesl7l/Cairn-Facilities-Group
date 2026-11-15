package com.jonesl7l.carinfg.core.data.session

import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.network.result.ApiError

sealed interface LoginResult {
    data class Success(val user: User) : LoginResult

    data object InvalidCredentials : LoginResult

    data object NotTechnician : LoginResult

    data class Failed(val error: ApiError) : LoginResult
}
