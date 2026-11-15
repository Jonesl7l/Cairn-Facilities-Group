package com.jonesl7l.carinfg.core.network.datasource

import com.jonesl7l.carinfg.core.network.dto.reference.ReferenceDataDto
import com.jonesl7l.carinfg.core.network.result.ApiResult
import kotlinx.coroutines.CompletableDeferred

class FakeReferenceRemoteDataSource(var result: ApiResult<ReferenceDataDto>) : ReferenceRemoteDataSource {
    var calls = 0
        private set

    var gate: CompletableDeferred<Unit>? = null

    override suspend fun getReferenceData(): ApiResult<ReferenceDataDto> {
        calls++
        gate?.await()
        return result
    }
}
