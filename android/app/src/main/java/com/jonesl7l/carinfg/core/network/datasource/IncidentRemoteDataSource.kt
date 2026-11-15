package com.jonesl7l.carinfg.core.network.datasource

import com.jonesl7l.carinfg.core.network.dto.incident.DeleteIncidentRequest
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentWriteRequest
import com.jonesl7l.carinfg.core.network.result.ApiResult

interface IncidentRemoteDataSource {
    suspend fun getChanges(afterChangeSeq: Long): ApiResult<IncidentChangesPage>

    suspend fun getIncident(id: String): ApiResult<IncidentDto>

    suspend fun createIncident(request: IncidentWriteRequest): ApiResult<IncidentDto>

    suspend fun updateIncident(
        id: String,
        version: Int,
        request: IncidentWriteRequest,
    ): ApiResult<IncidentDto>

    suspend fun deleteIncident(
        id: String,
        version: Int,
        request: DeleteIncidentRequest,
    ): ApiResult<IncidentDto>
}
