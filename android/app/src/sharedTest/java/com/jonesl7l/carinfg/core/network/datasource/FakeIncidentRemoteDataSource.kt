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

    val incidentResults = ArrayDeque<ApiResult<IncidentDto>>()
    val incidentRequests = mutableListOf<String>()

    val createResults = ArrayDeque<ApiResult<IncidentDto>>()
    val createRequests = mutableListOf<IncidentWriteRequest>()

    val updateResults = ArrayDeque<ApiResult<IncidentDto>>()
    val updateVersions = mutableListOf<Int>()

    val deleteResults = ArrayDeque<ApiResult<IncidentDto>>()
    val deleteVersions = mutableListOf<Int>()

    val uploadResults = ArrayDeque<ApiResult<PhotoDto>>()
    val uploadedPhotoIds = mutableListOf<String>()

    override suspend fun getChanges(afterChangeSeq: Long): ApiResult<IncidentChangesPage> {
        changeRequests += afterChangeSeq
        return changePages.removeFirst()
    }

    override suspend fun getIncident(id: String): ApiResult<IncidentDto> {
        incidentRequests += id
        return incidentResults.removeFirst()
    }

    override suspend fun createIncident(request: IncidentWriteRequest): ApiResult<IncidentDto> {
        createRequests += request
        return createResults.removeFirst()
    }

    override suspend fun updateIncident(
        id: String,
        version: Int,
        request: IncidentWriteRequest,
    ): ApiResult<IncidentDto> {
        updateVersions += version
        return updateResults.removeFirst()
    }

    override suspend fun deleteIncident(
        id: String,
        version: Int,
        request: DeleteIncidentRequest,
    ): ApiResult<IncidentDto> {
        deleteVersions += version
        return deleteResults.removeFirst()
    }

    override suspend fun uploadPhoto(
        incidentId: String,
        photoId: String,
        file: File,
    ): ApiResult<PhotoDto> {
        uploadedPhotoIds += photoId
        return uploadResults.removeFirst()
    }
}
