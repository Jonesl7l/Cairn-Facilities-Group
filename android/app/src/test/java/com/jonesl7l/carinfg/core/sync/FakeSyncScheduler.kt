package com.jonesl7l.carinfg.core.sync

import kotlin.time.Duration

class FakeSyncScheduler : SyncScheduler {
    var syncRequests = 0
    val delayedRequests = mutableListOf<Duration>()
    var cancelled = false

    override fun requestSync() {
        syncRequests++
    }

    override fun requestSyncAfter(delay: Duration) {
        delayedRequests += delay
    }

    override fun cancelAll() {
        cancelled = true
    }
}
