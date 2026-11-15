package com.jonesl7l.carinfg.core.sync

class FakeSyncScheduler : SyncScheduler {
    var syncRequests = 0
    var cancelled = false

    override fun requestSync() {
        syncRequests++
    }

    override fun cancelAll() {
        cancelled = true
    }
}
