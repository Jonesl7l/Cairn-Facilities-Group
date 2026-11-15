package com.jonesl7l.carinfg.core.network.datasource

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.network.ApiTestHelper.apiCallHandler
import com.jonesl7l.carinfg.core.network.ApiTestHelper.json
import com.jonesl7l.carinfg.core.network.ApiTestHelper.jsonResponse
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemDetailsDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemJson
import com.jonesl7l.carinfg.core.network.ApiTestHelper.referenceDataDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.retrofit
import com.jonesl7l.carinfg.core.network.http.HttpStatus
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.create

class ReferenceRemoteDataSourceImplTest {
    private val server = MockWebServer()
    private lateinit var dataSource: ReferenceRemoteDataSourceImpl

    @Before
    fun setUp() {
        server.start()
        dataSource =
            ReferenceRemoteDataSourceImpl(
                api = retrofit(server = server).create(),
                apiCallHandler = apiCallHandler(),
            )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `getReferenceData requests every list in one call and decodes them`() = runTest {
        server.enqueue(jsonResponse(body = json.encodeToString(referenceDataDto())))

        assertThat(dataSource.getReferenceData()).isEqualTo(ApiResult.Success(value = referenceDataDto()))
        assertThat(server.takeRequest().url.encodedPath).isEqualTo("/reference")
    }

    @Test
    fun `a missing list is unexpected`() = runTest {
        val body = JsonObject(content = json.encodeToJsonElement(referenceDataDto()).jsonObject - "technicians")
        server.enqueue(jsonResponse(body = body.toString()))

        assertThat(dataSource.getReferenceData())
            .isEqualTo(ApiResult.Failure(error = ApiError.Unexpected(status = null)))
    }

    @Test
    fun `an unauthenticated request fails with unauthenticated`() = runTest {
        val problem = problemDetailsDto(status = HttpStatus.UNAUTHORIZED)
        server.enqueue(
            jsonResponse(
                code = HttpStatus.UNAUTHORIZED,
                body = problemJson(problem = problem),
            ),
        )

        assertThat(dataSource.getReferenceData()).isEqualTo(ApiResult.Failure(error = ApiError.Unauthenticated))
    }
}
