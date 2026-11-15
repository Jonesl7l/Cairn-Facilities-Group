package com.jonesl7l.carinfg.core.sync

interface SyncScheduler {
    fun requestSync()

    fun cancelAll()
}
