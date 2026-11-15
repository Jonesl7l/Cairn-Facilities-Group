package com.jonesl7l.carinfg.core.sync

import kotlin.time.Duration

sealed interface SyncRunResult {
    data object Done : SyncRunResult

    data object RetryWithBackoff : SyncRunResult

    data class RetryAfter(val delay: Duration) : SyncRunResult

    data object SessionExpired : SyncRunResult
}
