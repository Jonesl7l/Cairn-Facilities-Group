package com.jonesl7l.carinfg.core.sync.push

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.network.ApiTestHelper.now
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemFieldErrorDto
import com.jonesl7l.carinfg.core.network.result.ApiError
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class PushErrorHelperTest {
    @Test
    fun `a 401 stops the pass for the session`() {
        assertThat(decide(error = ApiError.Unauthenticated)).isEqualTo(PushErrorAction.StopForSession)
    }

    @Test
    fun `network errors, 5xx and 503 stop the pass to retry`() {
        val errors =
            listOf(
                ApiError.Network(cause = IOException()),
                ApiError.Server(status = 500),
                ApiError.ServiceUnavailable(retryAfter = 30.seconds),
            )

        errors.forEach { error -> assertThat(decide(error = error)).isEqualTo(PushErrorAction.StopAndRetry) }
    }

    @Test
    fun `validation, too many photos and a photo too large need attention for validation`() {
        val expected = PushErrorAction.NeedsAttention(reason = ConflictReason.VALIDATION)
        val errors =
            listOf(
                ApiError.ValidationFailed(fieldErrors = listOf(problemFieldErrorDto())),
                ApiError.PhotoLimitExceeded,
                ApiError.PayloadTooLarge,
            )

        errors.forEach { error -> assertThat(decide(error = error)).isEqualTo(expected) }
    }

    @Test
    fun `a 409 needs attention as locked`() {
        val action = decide(error = ApiError.StatusLocked(currentStatus = "IN_PROGRESS"))

        assertThat(action).isEqualTo(PushErrorAction.NeedsAttention(reason = ConflictReason.STATUS_LOCKED))
    }

    @Test
    fun `a 410 needs attention as deleted remotely`() {
        val action = decide(error = incidentDeleted, operation = PushOperation.UPDATE)

        assertThat(action).isEqualTo(PushErrorAction.NeedsAttention(reason = ConflictReason.DELETED_REMOTELY))
    }

    @Test
    fun `a 410 on a delete removes the incident locally`() {
        val action = decide(error = incidentDeleted, operation = PushOperation.DELETE)

        assertThat(action).isEqualTo(PushErrorAction.RemoveLocally)
    }

    @Test
    fun `a 404 on an update submits it again as a new report`() {
        val action = decide(error = ApiError.NotFound, operation = PushOperation.UPDATE)

        assertThat(action).isEqualTo(PushErrorAction.ResubmitAsCreate)
    }

    @Test
    fun `a 404 on a delete removes the incident locally`() {
        val action = decide(error = ApiError.NotFound, operation = PushOperation.DELETE)

        assertThat(action).isEqualTo(PushErrorAction.RemoveLocally)
    }

    @Test
    fun `a 404 on a create or a photo upload is recorded as a failure`() {
        listOf(PushOperation.CREATE, PushOperation.PHOTO_UPLOAD).forEach { operation ->
            val action = decide(error = ApiError.NotFound, operation = operation)

            assertThat(action).isEqualTo(PushErrorAction.RecordFailure)
        }
    }

    @Test
    fun `a 412 that reaches the rules leaves the incident for the next pass`() {
        val action = decide(error = ApiError.VersionConflict(current = null))

        assertThat(action).isEqualTo(PushErrorAction.LeaveForNextPass)
    }

    @Test
    fun `errors that point to an app bug are recorded as failures`() {
        val errors =
            listOf(
                ApiError.InvalidCredentials,
                ApiError.Forbidden,
                ApiError.MalformedRequest,
                ApiError.PreconditionRequired,
                ApiError.UnsupportedMediaType,
                ApiError.Unexpected(status = 418),
            )

        errors.forEach { error -> assertThat(decide(error = error)).isEqualTo(PushErrorAction.RecordFailure) }
    }

    private val incidentDeleted =
        ApiError.IncidentDeleted(
            deletedAt = now,
            deletedBy = "operator1",
            referenceNumber = "INC-2026-00001",
        )

    private fun decide(
        error: ApiError,
        operation: PushOperation = PushOperation.UPDATE,
    ) = PushErrorHelper.decide(
        error = error,
        operation = operation,
    )
}
