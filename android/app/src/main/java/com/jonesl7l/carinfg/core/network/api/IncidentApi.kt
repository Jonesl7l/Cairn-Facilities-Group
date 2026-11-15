package com.jonesl7l.carinfg.core.network.api

import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentWriteRequest
import com.jonesl7l.carinfg.core.network.http.HttpHeaders
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface IncidentApi {
    @GET("incidents")
    suspend fun getChanges(
        @Query("changeSeq_gte") changeSeqFrom: Long,
        @Query("_sort") sort: String,
        @Query("_order") order: String,
        @Query("_limit") limit: Int,
    ): Response<List<IncidentDto>>

    @GET("incidents/{id}")
    suspend fun getIncident(@Path("id") id: String): Response<IncidentDto>

    @POST("incidents")
    suspend fun createIncident(@Body request: IncidentWriteRequest): Response<IncidentDto>

    @PATCH("incidents/{id}")
    suspend fun updateIncident(
        @Path("id") id: String,
        @Header(HttpHeaders.IF_MATCH) ifMatch: String,
        @Body request: IncidentWriteRequest,
    ): Response<IncidentDto>
}
