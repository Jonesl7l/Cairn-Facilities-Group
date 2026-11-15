package com.jonesl7l.carinfg.core.network.events

import kotlinx.coroutines.flow.Flow

interface SyncEventSource {
    fun events(): Flow<SyncEvent>
}
