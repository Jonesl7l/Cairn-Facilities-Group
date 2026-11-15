package com.jonesl7l.carinfg.core.network.datasource

import com.jonesl7l.carinfg.core.network.dto.reference.ReferenceDataDto
import com.jonesl7l.carinfg.core.network.result.ApiResult

interface ReferenceRemoteDataSource {
    suspend fun getReferenceData(): ApiResult<ReferenceDataDto>
}
