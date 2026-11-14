package com.jonesl7l.carinfg.core.network.api

import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface IncidentApi {
    @GET("incidents/{id}")
    suspend fun getIncident(@Path("id") id: String): Response<IncidentDto>
}
