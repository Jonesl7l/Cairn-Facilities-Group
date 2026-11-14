package com.jonesl7l.carinfg.core.network.datasource

import com.jonesl7l.carinfg.core.network.dto.user.UserDto
import com.jonesl7l.carinfg.core.network.result.ApiResult

interface AuthRemoteDataSource {
    suspend fun login(
        username: String,
        password: String,
    ): ApiResult<UserDto>
}
