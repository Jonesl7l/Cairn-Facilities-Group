package com.jonesl7l.carinfg.core.sync.push

import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import kotlin.time.Duration

sealed interface PushErrorAction {
    data class NeedsAttention(val reason: ConflictReason) : PushErrorAction

    data object RemoveLocally : PushErrorAction

    data object ResubmitAsCreate : PushErrorAction

    data object LeaveForNextPass : PushErrorAction

    data object RecordFailure : PushErrorAction

    data object StopForSession : PushErrorAction

    data class StopAndRetry(val retryAfter: Duration?) : PushErrorAction
}
