package com.jonesl7l.carinfg.core.sync

sealed interface SyncRunResult {
    data object Done : SyncRunResult

    data object RetryWithBackoff : SyncRunResult

    data object SessionExpired : SyncRunResult
}
