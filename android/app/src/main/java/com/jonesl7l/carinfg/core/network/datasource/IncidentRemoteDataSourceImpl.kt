package com.jonesl7l.carinfg.core.network.datasource

import com.jonesl7l.carinfg.core.network.api.IncidentApi
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.result.ApiCallHandler
import com.jonesl7l.carinfg.core.network.result.ApiResult
import javax.inject.Inject

class IncidentRemoteDataSourceImpl @Inject constructor(
    private val api: IncidentApi,
    private val apiCallHandler: ApiCallHandler,
) : IncidentRemoteDataSource {
    override suspend fun getIncident(id: String): ApiResult<IncidentDto> =
        apiCallHandler.execute(call = { api.getIncident(id = id) })
}
