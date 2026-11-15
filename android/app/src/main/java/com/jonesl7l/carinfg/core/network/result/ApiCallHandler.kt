package com.jonesl7l.carinfg.core.network.result

import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import retrofit2.Response
import timber.log.Timber

class ApiCallHandler @Inject constructor(private val problemParser: ProblemParser) {
    suspend fun <T : Any> execute(call: suspend () -> Response<T>): ApiResult<T> =
        execute(call = call, onSuccess = { it.body() })

    suspend fun <T, R : Any> execute(
        call: suspend () -> Response<T>,
        onSuccess: (Response<T>) -> R?,
    ): ApiResult<R> = try {
        val response = call()
        when {
            !response.isSuccessful -> failure(error = problemParser.parse(response = response))

            else -> onSuccess(response)?.let { ApiResult.Success(value = it) }
                ?: failure(error = ApiError.Unexpected(status = response.code()))
        }
    } catch (e: IOException) {
        failure(
            error = ApiError.Network(cause = e),
            cause = e,
        )
    } catch (e: SerializationException) {
        failure(
            error = ApiError.Unexpected(status = null),
            cause = e,
        )
    }

    private fun failure(
        error: ApiError,
        cause: Throwable? = null,
    ): ApiResult.Failure {
        if (error.category == ErrorCategory.BUG) {
            Timber.e(cause, "failure: call failed with %s", error)
        } else {
            Timber.w(cause, "failure: call failed with %s", error)
        }
        return ApiResult.Failure(error = error)
    }
}
