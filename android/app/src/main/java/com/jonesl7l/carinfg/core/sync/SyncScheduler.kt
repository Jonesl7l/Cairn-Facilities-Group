package com.jonesl7l.carinfg.core.sync

import kotlinx.coroutines.flow.Flow

interface SyncScheduler {
    val isSyncing: Flow<Boolean>

    fun requestSync()

    fun cancelAll()
}
