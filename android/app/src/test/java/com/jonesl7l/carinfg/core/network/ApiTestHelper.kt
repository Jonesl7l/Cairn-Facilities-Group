package com.jonesl7l.carinfg.core.network

import com.jonesl7l.carinfg.core.di.NetworkModule
import com.jonesl7l.carinfg.core.network.dto.problem.ProblemDetailsDto
import com.jonesl7l.carinfg.core.network.dto.problem.ProblemFieldErrorDto
import com.jonesl7l.carinfg.core.network.dto.user.UserDto
import com.jonesl7l.carinfg.core.network.http.HttpHeaders
import com.jonesl7l.carinfg.core.network.http.MediaTypes
import com.jonesl7l.carinfg.core.network.result.ApiCallHandler
import com.jonesl7l.carinfg.core.network.result.ProblemParser
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ApiTestHelper {
    val now: Instant = Instant.parse("2026-11-13T12:00:00Z")
    const val REPORTER_ID = "tech1"

    val json: Json = NetworkModule.provideJson()

    private val problemJsonMediaType = MediaTypes.PROBLEM_JSON.toMediaType()

    fun retrofit(server: MockWebServer): Retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(OkHttpClient())
        .addConverterFactory(json.asConverterFactory(MediaTypes.JSON.toMediaType()))
        .build()

    fun apiCallHandler(): ApiCallHandler = ApiCallHandler(problemParser = ProblemParser(json = json))

    fun jsonResponse(
        code: Int = 200,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): MockResponse = MockResponse.Builder()
        .code(code)
        .addHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.JSON)
        .apply { headers.forEach { (name, value) -> addHeader(name, value) } }
        .body(body)
        .build()

    fun errorResponse(
        status: Int,
        body: String = "",
        headers: Map<String, String> = emptyMap(),
    ): Response<Unit> {
        val raw = okhttp3.Response.Builder()
            .code(status)
            .message("Error")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/").build())
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
        return Response.error(body.toResponseBody(problemJsonMediaType), raw)
    }

    fun problemJson(problem: ProblemDetailsDto): String = json.encodeToString(problem)

    fun userDto(
        id: String = "u1",
        username: String = REPORTER_ID,
        displayName: String = "Tech One",
        role: String = "TECHNICIAN",
    ) = UserDto(
        id = id,
        username = username,
        displayName = displayName,
        role = role,
    )

    fun problemDetailsDto(
        status: Int,
        type: String? = "about:blank",
        title: String? = "Problem",
        detail: String? = null,
        code: String? = null,
        deletedAt: Instant? = null,
        deletedBy: String? = null,
        referenceNumber: String? = null,
        currentStatus: String? = null,
        errors: List<ProblemFieldErrorDto> = emptyList(),
    ) = ProblemDetailsDto(
        type = type,
        title = title,
        status = status,
        detail = detail,
        code = code,
        deletedAt = deletedAt,
        deletedBy = deletedBy,
        referenceNumber = referenceNumber,
        currentStatus = currentStatus,
        errors = errors,
    )
}
