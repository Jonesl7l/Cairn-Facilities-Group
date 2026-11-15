package com.jonesl7l.carinfg.core.network.datasource

import com.jonesl7l.carinfg.core.network.api.AuthApi
import com.jonesl7l.carinfg.core.network.dto.user.UserDto
import com.jonesl7l.carinfg.core.network.result.ApiCallHandler
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import javax.inject.Inject
import timber.log.Timber

class AuthRemoteDataSourceImpl @Inject constructor(
    private val api: AuthApi,
    private val apiCallHandler: ApiCallHandler,
) : AuthRemoteDataSource {
    override suspend fun login(
        username: String,
        password: String,
    ): ApiResult<UserDto> {
        val result = apiCallHandler.execute(call = {
            api.findUsers(
                username = username,
                password = password,
            )
        })
        return when (result) {
            is ApiResult.Failure -> result

            is ApiResult.Success -> {
                val user = result.value.firstOrNull() ?: return invalidCredentials(username = username)
                ApiResult.Success(value = user)
            }
        }
    }

    private fun invalidCredentials(username: String): ApiResult.Failure {
        Timber.i("invalidCredentials: rejected credentials for %s", username)
        return ApiResult.Failure(error = ApiError.InvalidCredentials)
    }
}
