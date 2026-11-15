package com.jonesl7l.carinfg.core.network.datasource

import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.result.ApiResult

interface IncidentRemoteDataSource {
    suspend fun getChanges(afterChangeSeq: Long): ApiResult<IncidentChangesPage>

    suspend fun getIncident(id: String): ApiResult<IncidentDto>
}
