package com.jonesl7l.carinfg.core.network.api

import com.jonesl7l.carinfg.core.network.dto.user.UserDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AuthApi {
    /** Mock login that looks up the user by credentials in json-server; TODO replace with a token-based auth endpoint. */
    @GET("users")
    suspend fun findUsers(
        @Query("username") username: String,
        @Query("password") password: String,
    ): Response<List<UserDto>>
}
