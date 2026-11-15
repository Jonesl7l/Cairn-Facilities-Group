package com.jonesl7l.carinfg.core.network.datasource

import com.jonesl7l.carinfg.core.network.api.ReferenceApi
import com.jonesl7l.carinfg.core.network.dto.reference.ReferenceDataDto
import com.jonesl7l.carinfg.core.network.result.ApiCallHandler
import com.jonesl7l.carinfg.core.network.result.ApiResult
import javax.inject.Inject

class ReferenceRemoteDataSourceImpl @Inject constructor(
    private val api: ReferenceApi,
    private val apiCallHandler: ApiCallHandler,
) : ReferenceRemoteDataSource {
    override suspend fun getReferenceData(): ApiResult<ReferenceDataDto> =
        apiCallHandler.execute(call = { api.getReferenceData() })
}
