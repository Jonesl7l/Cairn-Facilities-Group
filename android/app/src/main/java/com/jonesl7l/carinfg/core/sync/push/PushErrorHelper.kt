package com.jonesl7l.carinfg.core.sync.push

import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.network.result.ApiError

object PushErrorHelper {
    fun decide(
        error: ApiError,
        operation: PushOperation,
    ): PushErrorAction = when (error) {
        ApiError.Unauthenticated -> PushErrorAction.StopForSession

        is ApiError.Network, is ApiError.Server, is ApiError.ServiceUnavailable -> PushErrorAction.StopAndRetry

        is ApiError.ValidationFailed,
        ApiError.PhotoLimitExceeded,
        ApiError.PayloadTooLarge,
        -> PushErrorAction.NeedsAttention(reason = ConflictReason.VALIDATION)

        is ApiError.StatusLocked -> PushErrorAction.NeedsAttention(reason = ConflictReason.STATUS_LOCKED)

        is ApiError.IncidentDeleted -> if (operation == PushOperation.DELETE) {
            PushErrorAction.RemoveLocally
        } else {
            PushErrorAction.NeedsAttention(reason = ConflictReason.DELETED_REMOTELY)
        }

        ApiError.NotFound -> when (operation) {
            PushOperation.UPDATE -> PushErrorAction.ResubmitAsCreate
            PushOperation.DELETE -> PushErrorAction.RemoveLocally
            PushOperation.CREATE, PushOperation.PHOTO_UPLOAD -> PushErrorAction.RecordFailure
        }

        is ApiError.VersionConflict -> PushErrorAction.LeaveForNextPass

        ApiError.InvalidCredentials,
        ApiError.Forbidden,
        ApiError.MalformedRequest,
        ApiError.PreconditionRequired,
        ApiError.UnsupportedMediaType,
        is ApiError.Unexpected,
        -> PushErrorAction.RecordFailure
    }
}
