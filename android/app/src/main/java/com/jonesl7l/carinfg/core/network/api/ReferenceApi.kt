package com.jonesl7l.carinfg.core.network.api

import com.jonesl7l.carinfg.core.network.dto.reference.ReferenceDataDto
import retrofit2.Response
import retrofit2.http.GET

interface ReferenceApi {
    @GET("reference")
    suspend fun getReferenceData(): Response<ReferenceDataDto>
}
