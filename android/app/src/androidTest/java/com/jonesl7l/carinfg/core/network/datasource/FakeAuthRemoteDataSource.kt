package com.jonesl7l.carinfg.core.network.datasource

import com.jonesl7l.carinfg.core.network.dto.user.UserDto
import com.jonesl7l.carinfg.core.network.result.ApiResult

class FakeAuthRemoteDataSource(var result: ApiResult<UserDto>) : AuthRemoteDataSource {
    override suspend fun login(
        username: String,
        password: String,
    ): ApiResult<UserDto> = result
}
