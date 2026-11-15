package com.jonesl7l.carinfg.core.network.datasource

import com.jonesl7l.carinfg.core.network.api.IncidentApi
import com.jonesl7l.carinfg.core.network.dto.incident.DeleteIncidentRequest
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentWriteRequest
import com.jonesl7l.carinfg.core.network.dto.photo.PhotoDto
import com.jonesl7l.carinfg.core.network.http.HttpHeaders
import com.jonesl7l.carinfg.core.network.http.MediaTypes
import com.jonesl7l.carinfg.core.network.result.ApiCallHandler
import com.jonesl7l.carinfg.core.network.result.ApiResult
import java.io.File
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response

class IncidentRemoteDataSourceImpl @Inject constructor(
    private val api: IncidentApi,
    private val apiCallHandler: ApiCallHandler,
) : IncidentRemoteDataSource {
    override suspend fun getChanges(afterChangeSeq: Long): ApiResult<IncidentChangesPage> = apiCallHandler.execute(
        call = {
            api.getChanges(
                // json-server 0.17 only has an inclusive _gte; v1 adds :gt but is still a poorly documented beta
                changeSeqFrom = afterChangeSeq + 1,
                sort = SORT_FIELD,
                order = ORDER_ASC,
                limit = PAGE_SIZE,
            )
        },
        onSuccess = ::toChangesPage,
    )

    override suspend fun getIncident(id: String): ApiResult<IncidentDto> = apiCallHandler.execute(call = { api.getIncident(id = id) })

    override suspend fun createIncident(request: IncidentWriteRequest): ApiResult<IncidentDto> =
        apiCallHandler.execute(call = { api.createIncident(request = request) })

    override suspend fun updateIncident(
        id: String,
        version: Int,
        request: IncidentWriteRequest,
    ): ApiResult<IncidentDto> = apiCallHandler.execute(
        call = {
            api.updateIncident(
                id = id,
                ifMatch = entityTag(version = version),
                request = request,
            )
        },
    )

    override suspend fun deleteIncident(
        id: String,
        version: Int,
        request: DeleteIncidentRequest,
    ): ApiResult<IncidentDto> = apiCallHandler.execute(
        call = {
            api.deleteIncident(
                id = id,
                ifMatch = entityTag(version = version),
                request = request,
            )
        },
    )

    override suspend fun uploadPhoto(
        incidentId: String,
        photoId: String,
        file: File,
    ): ApiResult<PhotoDto> {
        val part =
            MultipartBody.Part.createFormData(
                name = PHOTO_PART_NAME,
                filename = file.name,
                body = file.asRequestBody(contentType = MediaTypes.JPEG.toMediaType()),
            )
        return apiCallHandler.execute(
            call = {
                api.uploadPhoto(
                    id = incidentId,
                    photoId = photoId,
                    file = part,
                )
            },
        )
    }

    private fun toChangesPage(response: Response<List<IncidentDto>>): IncidentChangesPage? {
        val incidents = response.body() ?: return null
        val databaseId = response.headers()[HttpHeaders.DATABASE_ID] ?: return null
        return IncidentChangesPage(
            incidents = incidents,
            databaseId = databaseId,
            isLastPage = incidents.size < PAGE_SIZE,
        )
    }

    private fun entityTag(version: Int): String = "\"$version\""

    private companion object {
        const val PAGE_SIZE = 50
        const val SORT_FIELD = "changeSeq"
        const val ORDER_ASC = "asc"
        const val PHOTO_PART_NAME = "file"
    }
}
