package com.jonesl7l.carinfg.core.network.datasource

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.jonesl7l.carinfg.core.network.ApiTestHelper.apiCallHandler
import com.jonesl7l.carinfg.core.network.ApiTestHelper.json
import com.jonesl7l.carinfg.core.network.ApiTestHelper.jsonResponse
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemDetailsDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemJson
import com.jonesl7l.carinfg.core.network.ApiTestHelper.retrofit
import com.jonesl7l.carinfg.core.network.ApiTestHelper.userDto
import com.jonesl7l.carinfg.core.network.http.HttpStatus
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.create

class AuthRemoteDataSourceImplTest {
    private val server = MockWebServer()
    private lateinit var dataSource: AuthRemoteDataSourceImpl

    @Before
    fun setUp() {
        server.start()
        dataSource =
            AuthRemoteDataSourceImpl(
                api = retrofit(server = server).create(),
                apiCallHandler = apiCallHandler(),
            )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `login sends the credentials and returns the matching user`() = runTest {
        server.enqueue(jsonResponse(body = json.encodeToString(listOf(userDto()))))

        val result = dataSource.login(
            username = "tech1",
            password = "password",
        )

        assertThat(result).isEqualTo(ApiResult.Success(value = userDto()))
        val url = server.takeRequest().url
        assertThat(url.encodedPath).isEqualTo("/users")
        assertThat(url.queryParameter("username")).isEqualTo("tech1")
        assertThat(url.queryParameter("password")).isEqualTo("password")
    }

    @Test
    fun `an empty list means invalid credentials`() = runTest {
        server.enqueue(jsonResponse(body = "[]"))

        assertThat(
            dataSource.login(
                username = "tech1",
                password = "wrong",
            ),
        )
            .isEqualTo(ApiResult.Failure(error = ApiError.InvalidCredentials))
    }

    @Test
    fun `an error response comes back as a typed failure`() = runTest {
        val problem = problemDetailsDto(status = HttpStatus.SERVICE_UNAVAILABLE)
        server.enqueue(
            jsonResponse(
                code = HttpStatus.SERVICE_UNAVAILABLE,
                body = problemJson(problem = problem),
            ),
        )

        assertThat(
            dataSource.login(
                username = "tech1",
                password = "password",
            ),
        )
            .isEqualTo(ApiResult.Failure(error = ApiError.ServiceUnavailable(retryAfter = null)))
    }

    @Test
    fun `a connection failure is a network error`() = runTest {
        server.close()

        assertThat(
            dataSource.login(
                username = "tech1",
                password = "password",
            ),
        )
            .isInstanceOf<ApiResult.Failure>()
            .prop(ApiResult.Failure::error)
            .isInstanceOf<ApiError.Network>()
    }

    @Test
    fun `an undecodable body is unexpected`() = runTest {
        server.enqueue(jsonResponse(body = """[{"id":"u1"}]"""))

        assertThat(
            dataSource.login(
                username = "tech1",
                password = "password",
            ),
        )
            .isEqualTo(ApiResult.Failure(error = ApiError.Unexpected(status = null)))
    }
}
