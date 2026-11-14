package com.jonesl7l.carinfg.core.network.datasource

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.jonesl7l.carinfg.core.network.ApiTestHelper.INCIDENT_ID
import com.jonesl7l.carinfg.core.network.ApiTestHelper.apiCallHandler
import com.jonesl7l.carinfg.core.network.ApiTestHelper.incidentDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.json
import com.jonesl7l.carinfg.core.network.ApiTestHelper.jsonResponse
import com.jonesl7l.carinfg.core.network.ApiTestHelper.now
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemDetailsDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemJson
import com.jonesl7l.carinfg.core.network.ApiTestHelper.retrofit
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.http.HttpStatus
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.create

class IncidentRemoteDataSourceImplTest {
    private val server = MockWebServer()
    private lateinit var dataSource: IncidentRemoteDataSourceImpl

    @Before
    fun setUp() {
        server.start()
        dataSource =
            IncidentRemoteDataSourceImpl(
                api = retrofit(server = server).create(),
                apiCallHandler = apiCallHandler(),
            )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `getIncident requests the incident by id and decodes every field`() = runTest {
        server.enqueue(jsonResponse(body = json.encodeToString(incidentDto())))

        val result = dataSource.getIncident(id = INCIDENT_ID)

        assertThat(result).isEqualTo(ApiResult.Success(value = incidentDto()))
        assertThat(server.takeRequest().url.encodedPath).isEqualTo("/incidents/$INCIDENT_ID")
    }

    @Test
    fun `optional fields missing from the body take their defaults`() = runTest {
        val body = incidentJson(without = optionalKeys)
        server.enqueue(jsonResponse(body = body.toString()))

        val expected =
            incidentDto(
                gpsLocation = null,
                buildingId = null,
                floorId = null,
                roomId = null,
                exactSpot = null,
                assetId = null,
                equipmentOutOfService = false,
                immediateActions = emptyList(),
                immediateActionsOther = null,
                persons = emptyList(),
                photos = emptyList(),
                additionalNotes = null,
                assignedTo = null,
                dueDate = null,
                rootCause = null,
                correctiveActions = null,
                regulatoryReportable = null,
                supervisorAcknowledged = null,
            )
        assertThat(dataSource.getIncident(id = INCIDENT_ID)).isEqualTo(ApiResult.Success(value = expected))
    }

    @Test
    fun `unknown keys are ignored and unrecognised values come through as is`() = runTest {
        val incident = incidentDto(severity = "CATASTROPHIC", status = "ON_HOLD")
        val unknownKey = "priority" to JsonPrimitive(value = "P1")
        val body = JsonObject(content = incidentJson(incident = incident) + unknownKey)
        server.enqueue(jsonResponse(body = body.toString()))

        assertThat(dataSource.getIncident(id = INCIDENT_ID)).isEqualTo(ApiResult.Success(value = incident))
    }

    @Test
    fun `a missing required field is unexpected`() = runTest {
        val body = incidentJson(without = setOf("status"))
        server.enqueue(jsonResponse(body = body.toString()))

        assertThat(dataSource.getIncident(id = INCIDENT_ID))
            .isEqualTo(ApiResult.Failure(error = ApiError.Unexpected(status = null)))
    }

    @Test
    fun `json-server's own 404 is not found`() = runTest {
        server.enqueue(jsonResponse(code = HttpStatus.NOT_FOUND, body = "{}"))

        assertThat(dataSource.getIncident(id = INCIDENT_ID))
            .isEqualTo(ApiResult.Failure(error = ApiError.NotFound))
    }

    @Test
    fun `410 carries who deleted it`() = runTest {
        val problem =
            problemDetailsDto(
                status = HttpStatus.GONE,
                code = "INCIDENT_DELETED",
                deletedAt = now,
                deletedBy = "operator1",
                referenceNumber = "INC-2026-00001",
            )
        server.enqueue(
            jsonResponse(
                code = HttpStatus.GONE,
                body = problemJson(problem = problem),
            ),
        )

        val expected =
            ApiError.IncidentDeleted(
                deletedAt = now,
                deletedBy = "operator1",
                referenceNumber = "INC-2026-00001",
            )
        assertThat(dataSource.getIncident(id = INCIDENT_ID)).isEqualTo(ApiResult.Failure(error = expected))
    }

    @Test
    fun `a connection failure is a network error`() = runTest {
        server.close()

        assertThat(dataSource.getIncident(id = INCIDENT_ID))
            .isInstanceOf<ApiResult.Failure>()
            .prop(ApiResult.Failure::error)
            .isInstanceOf<ApiError.Network>()
    }

    private fun incidentJson(
        incident: IncidentDto = incidentDto(),
        without: Set<String> = emptySet(),
    ): JsonObject = JsonObject(content = json.encodeToJsonElement(incident).jsonObject - without)

    private companion object {
        val optionalKeys =
            setOf(
                "gpsLocation",
                "buildingId",
                "floorId",
                "roomId",
                "exactSpot",
                "assetId",
                "equipmentOutOfService",
                "immediateActions",
                "immediateActionsOther",
                "persons",
                "photos",
                "additionalNotes",
                "assignedTo",
                "dueDate",
                "rootCause",
                "correctiveActions",
                "regulatoryReportable",
                "supervisorAcknowledged",
            )
    }
}
