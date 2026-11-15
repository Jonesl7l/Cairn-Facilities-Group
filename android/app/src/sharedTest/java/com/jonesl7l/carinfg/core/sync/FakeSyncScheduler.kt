package com.jonesl7l.carinfg.core.sync

import kotlinx.coroutines.flow.MutableStateFlow

class FakeSyncScheduler : SyncScheduler {
    override val isSyncing = MutableStateFlow(false)
    var syncRequests = 0
    var cancelled = false

    override fun requestSync() {
        syncRequests++
    }

    override fun cancelAll() {
        cancelled = true
    }
}
