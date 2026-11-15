package com.jonesl7l.carinfg.core.sync

import kotlin.time.Duration

interface SyncScheduler {
    fun requestSync()

    fun requestSyncAfter(delay: Duration)

    fun cancelAll()
}
