package com.jonesl7l.carinfg.core.sync.push

import kotlin.time.Duration

sealed interface PushResult {
    data object Success : PushResult

    data object NoSession : PushResult

    data object SessionExpired : PushResult

    data class Retry(val retryAfter: Duration?) : PushResult
}
