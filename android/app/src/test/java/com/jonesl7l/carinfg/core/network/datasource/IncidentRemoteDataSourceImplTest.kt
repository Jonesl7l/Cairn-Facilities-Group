package com.jonesl7l.carinfg.core.network.datasource

import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.jonesl7l.carinfg.core.network.ApiTestHelper.DATABASE_ID
import com.jonesl7l.carinfg.core.network.ApiTestHelper.INCIDENT_ID
import com.jonesl7l.carinfg.core.network.ApiTestHelper.REPORTER_ID
import com.jonesl7l.carinfg.core.network.ApiTestHelper.apiCallHandler
import com.jonesl7l.carinfg.core.network.ApiTestHelper.deleteIncidentRequest
import com.jonesl7l.carinfg.core.network.ApiTestHelper.incidentDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.incidentWriteRequest
import com.jonesl7l.carinfg.core.network.ApiTestHelper.json
import com.jonesl7l.carinfg.core.network.ApiTestHelper.jsonResponse
import com.jonesl7l.carinfg.core.network.ApiTestHelper.now
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemDetailsDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemFieldErrorDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemJson
import com.jonesl7l.carinfg.core.network.ApiTestHelper.retrofit
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.http.HttpHeaders
import com.jonesl7l.carinfg.core.network.http.HttpStatus
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import mockwebserver3.MockResponse
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
    fun `getChanges requests the page after the cursor, oldest first`() = runTest {
        server.enqueue(changesResponse(incidents = emptyList()))

        dataSource.getChanges(afterChangeSeq = 42)

        val url = server.takeRequest().url
        assertThat(url.encodedPath).isEqualTo("/incidents")
        assertThat(url.queryParameter("changeSeq_gte")).isEqualTo("43")
        assertThat(url.queryParameter("_sort")).isEqualTo("changeSeq")
        assertThat(url.queryParameter("_order")).isEqualTo("asc")
        assertThat(url.queryParameter("_limit")).isEqualTo("50")
    }

    @Test
    fun `a short page is the last page and carries the database id`() = runTest {
        val incidents = listOf(incidentDto())
        server.enqueue(changesResponse(incidents = incidents))

        val expected =
            IncidentChangesPage(
                incidents = incidents,
                databaseId = DATABASE_ID,
                isLastPage = true,
            )
        assertThat(dataSource.getChanges(afterChangeSeq = 0)).isEqualTo(ApiResult.Success(value = expected))
    }

    @Test
    fun `a full page is not the last page`() = runTest {
        val incidents = List(size = 50) { incidentDto(id = "incident-$it", changeSeq = it + 1L) }
        server.enqueue(changesResponse(incidents = incidents))

        assertThat(dataSource.getChanges(afterChangeSeq = 0))
            .isInstanceOf<ApiResult.Success<IncidentChangesPage>>()
            .prop(ApiResult.Success<IncidentChangesPage>::value)
            .prop(IncidentChangesPage::isLastPage)
            .isEqualTo(false)
    }

    @Test
    fun `an empty page is the last page`() = runTest {
        server.enqueue(changesResponse(incidents = emptyList()))

        assertThat(dataSource.getChanges(afterChangeSeq = 0))
            .isInstanceOf<ApiResult.Success<IncidentChangesPage>>()
            .prop(ApiResult.Success<IncidentChangesPage>::value)
            .prop(IncidentChangesPage::isLastPage)
            .isEqualTo(true)
    }

    @Test
    fun `deleted incidents come through as tombstones`() = runTest {
        val tombstone =
            incidentDto(
                deletedAt = now,
                deletedBy = "operator1",
            )
        server.enqueue(changesResponse(incidents = listOf(tombstone)))

        assertThat(dataSource.getChanges(afterChangeSeq = 0))
            .isInstanceOf<ApiResult.Success<IncidentChangesPage>>()
            .prop(ApiResult.Success<IncidentChangesPage>::value)
            .prop(IncidentChangesPage::incidents)
            .isEqualTo(listOf(tombstone))
    }

    @Test
    fun `a page without the database id header is unexpected`() = runTest {
        server.enqueue(changesResponse(incidents = emptyList(), databaseId = null))

        assertThat(dataSource.getChanges(afterChangeSeq = 0))
            .isEqualTo(ApiResult.Failure(error = ApiError.Unexpected(status = HttpStatus.OK)))
    }

    @Test
    fun `an unauthenticated change request fails with unauthenticated`() = runTest {
        val problem = problemDetailsDto(status = HttpStatus.UNAUTHORIZED)
        server.enqueue(
            jsonResponse(
                code = HttpStatus.UNAUTHORIZED,
                body = problemJson(problem = problem),
            ),
        )

        assertThat(dataSource.getChanges(afterChangeSeq = 0))
            .isEqualTo(ApiResult.Failure(error = ApiError.Unauthenticated))
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

    @Test
    fun `createIncident posts every app-owned field, including cleared ones as null`() = runTest {
        val request = incidentWriteRequest(buildingId = null, unlistedLocation = "Portakabin")
        server.enqueue(
            jsonResponse(
                code = HttpStatus.CREATED,
                body = json.encodeToString(incidentDto()),
            ),
        )

        val result = dataSource.createIncident(request = request)

        assertThat(result).isEqualTo(ApiResult.Success(value = incidentDto()))
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.url.encodedPath).isEqualTo("/incidents")
        val body = json.parseToJsonElement(recorded.body!!.utf8()).jsonObject
        assertThat(body).isEqualTo(json.encodeToJsonElement(request).jsonObject)
        assertThat(body["buildingId"]).isEqualTo(JsonNull)
    }

    @Test
    fun `createIncident treats 200 for an existing id as success`() = runTest {
        server.enqueue(jsonResponse(body = json.encodeToString(incidentDto())))

        assertThat(dataSource.createIncident(request = incidentWriteRequest()))
            .isEqualTo(ApiResult.Success(value = incidentDto()))
    }

    @Test
    fun `createIncident 422 carries the field errors`() = runTest {
        val fieldError = problemFieldErrorDto()
        val problem =
            problemDetailsDto(
                status = HttpStatus.UNPROCESSABLE_CONTENT,
                errors = listOf(fieldError),
            )
        server.enqueue(
            jsonResponse(
                code = HttpStatus.UNPROCESSABLE_CONTENT,
                body = problemJson(problem = problem),
            ),
        )

        assertThat(dataSource.createIncident(request = incidentWriteRequest()))
            .isEqualTo(ApiResult.Failure(error = ApiError.ValidationFailed(fieldErrors = listOf(fieldError))))
    }

    @Test
    fun `createIncident 410 means the id was deleted`() = runTest {
        val problem =
            problemDetailsDto(
                status = HttpStatus.GONE,
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
        assertThat(dataSource.createIncident(request = incidentWriteRequest()))
            .isEqualTo(ApiResult.Failure(error = expected))
    }

    @Test
    fun `updateIncident patches with the version as a quoted If-Match`() = runTest {
        server.enqueue(jsonResponse(body = json.encodeToString(incidentDto(version = 4))))

        val result =
            dataSource.updateIncident(
                id = INCIDENT_ID,
                version = 3,
                request = incidentWriteRequest(),
            )

        assertThat(result).isEqualTo(ApiResult.Success(value = incidentDto(version = 4)))
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("PATCH")
        assertThat(recorded.url.encodedPath).isEqualTo("/incidents/$INCIDENT_ID")
        assertThat(recorded.headers[HttpHeaders.IF_MATCH]).isEqualTo("\"3\"")
        val bodyKeys = json.parseToJsonElement(recorded.body!!.utf8()).jsonObject.keys
        assertThat(bodyKeys).doesNotContain("status")
        assertThat(bodyKeys).doesNotContain("version")
    }

    @Test
    fun `updateIncident 412 carries the current server copy`() = runTest {
        val current = incidentDto(version = 5)
        val problem =
            problemDetailsDto(
                status = HttpStatus.PRECONDITION_FAILED,
                current = current,
            )
        server.enqueue(
            jsonResponse(
                code = HttpStatus.PRECONDITION_FAILED,
                body = problemJson(problem = problem),
            ),
        )

        val result =
            dataSource.updateIncident(
                id = INCIDENT_ID,
                version = 3,
                request = incidentWriteRequest(),
            )

        assertThat(result).isEqualTo(ApiResult.Failure(error = ApiError.VersionConflict(current = current)))
    }

    @Test
    fun `updateIncident 409 carries the current status`() = runTest {
        val problem =
            problemDetailsDto(
                status = HttpStatus.CONFLICT,
                currentStatus = "IN_PROGRESS",
            )
        server.enqueue(
            jsonResponse(
                code = HttpStatus.CONFLICT,
                body = problemJson(problem = problem),
            ),
        )

        val result =
            dataSource.updateIncident(
                id = INCIDENT_ID,
                version = 3,
                request = incidentWriteRequest(),
            )

        assertThat(result).isEqualTo(ApiResult.Failure(error = ApiError.StatusLocked(currentStatus = "IN_PROGRESS")))
    }

    @Test
    fun `updateIncident maps a missing record and a missing If-Match`() = runTest {
        val cases =
            listOf(
                HttpStatus.NOT_FOUND to ApiError.NotFound,
                HttpStatus.PRECONDITION_REQUIRED to ApiError.PreconditionRequired,
            )
        cases.forEach { (status, expected) ->
            server.enqueue(
                jsonResponse(
                    code = status,
                    body = problemJson(problem = problemDetailsDto(status = status)),
                ),
            )

            val result =
                dataSource.updateIncident(
                    id = INCIDENT_ID,
                    version = 3,
                    request = incidentWriteRequest(),
                )

            assertThat(result).isEqualTo(ApiResult.Failure(error = expected))
        }
    }

    @Test
    fun `deleteIncident patches only the deletion fields with a quoted If-Match`() = runTest {
        val request = deleteIncidentRequest()
        val tombstone =
            incidentDto(
                version = 4,
                deletedAt = now,
                deletedBy = REPORTER_ID,
            )
        server.enqueue(jsonResponse(body = json.encodeToString(tombstone)))

        val result =
            dataSource.deleteIncident(
                id = INCIDENT_ID,
                version = 3,
                request = request,
            )

        assertThat(result).isEqualTo(ApiResult.Success(value = tombstone))
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("PATCH")
        assertThat(recorded.url.encodedPath).isEqualTo("/incidents/$INCIDENT_ID")
        assertThat(recorded.headers[HttpHeaders.IF_MATCH]).isEqualTo("\"3\"")
        val body = json.parseToJsonElement(recorded.body!!.utf8()).jsonObject
        assertThat(body).isEqualTo(json.encodeToJsonElement(request).jsonObject)
        assertThat(body.keys).isEqualTo(setOf("deletedAt", "deletedBy"))
    }

    @Test
    fun `deleteIncident maps conflict, already deleted and status lock`() = runTest {
        val current = incidentDto(version = 5)
        val cases =
            listOf(
                problemDetailsDto(
                    status = HttpStatus.PRECONDITION_FAILED,
                    current = current,
                ) to ApiError.VersionConflict(current = current),
                problemDetailsDto(
                    status = HttpStatus.GONE,
                    deletedAt = now,
                    deletedBy = "operator1",
                    referenceNumber = "INC-2026-00001",
                ) to ApiError.IncidentDeleted(
                    deletedAt = now,
                    deletedBy = "operator1",
                    referenceNumber = "INC-2026-00001",
                ),
                problemDetailsDto(
                    status = HttpStatus.CONFLICT,
                    currentStatus = "ASSIGNED",
                ) to ApiError.StatusLocked(currentStatus = "ASSIGNED"),
            )
        cases.forEach { (problem, expected) ->
            server.enqueue(
                jsonResponse(
                    code = problem.status!!,
                    body = problemJson(problem = problem),
                ),
            )

            val result =
                dataSource.deleteIncident(
                    id = INCIDENT_ID,
                    version = 3,
                    request = deleteIncidentRequest(),
                )

            assertThat(result).isEqualTo(ApiResult.Failure(error = expected))
        }
    }

    private fun changesResponse(
        incidents: List<IncidentDto>,
        databaseId: String? = DATABASE_ID,
    ): MockResponse = jsonResponse(
        body = json.encodeToString(incidents),
        headers = databaseId?.let { mapOf(HttpHeaders.DATABASE_ID to it) } ?: emptyMap(),
    )

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
