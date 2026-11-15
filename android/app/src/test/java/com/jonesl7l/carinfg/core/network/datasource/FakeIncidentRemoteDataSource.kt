package com.jonesl7l.carinfg.core.network.datasource

import com.jonesl7l.carinfg.core.network.dto.incident.DeleteIncidentRequest
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentWriteRequest
import com.jonesl7l.carinfg.core.network.dto.photo.PhotoDto
import com.jonesl7l.carinfg.core.network.result.ApiResult
import java.io.File

class FakeIncidentRemoteDataSource : IncidentRemoteDataSource {
    val changePages = ArrayDeque<ApiResult<IncidentChangesPage>>()
    val changeRequests = mutableListOf<Long>()

    override suspend fun getChanges(afterChangeSeq: Long): ApiResult<IncidentChangesPage> {
        changeRequests += afterChangeSeq
        return changePages.removeFirst()
    }

    override suspend fun getIncident(id: String): ApiResult<IncidentDto> = unused()

    override suspend fun createIncident(request: IncidentWriteRequest): ApiResult<IncidentDto> = unused()

    override suspend fun updateIncident(
        id: String,
        version: Int,
        request: IncidentWriteRequest,
    ): ApiResult<IncidentDto> = unused()

    override suspend fun deleteIncident(
        id: String,
        version: Int,
        request: DeleteIncidentRequest,
    ): ApiResult<IncidentDto> = unused()

    override suspend fun uploadPhoto(
        incidentId: String,
        photoId: String,
        file: File,
    ): ApiResult<PhotoDto> = unused()

    private fun unused(): Nothing = throw UnsupportedOperationException("not used by these tests")
}
