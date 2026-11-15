package com.jonesl7l.carinfg.core.network.events

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

class FakeSyncEventSource : SyncEventSource {
    val streams = ArrayDeque<Flow<SyncEvent>>()
    var connections = 0
    var openStreams = 0

    override fun events(): Flow<SyncEvent> {
        connections++
        val stream = streams.removeFirstOrNull() ?: openStream()
        return stream
            .onStart { openStreams++ }
            .onCompletion { openStreams-- }
    }

    private fun openStream(): Flow<SyncEvent> = flow {
        emit(SyncEvent.Connected)
        awaitCancellation()
    }
}
