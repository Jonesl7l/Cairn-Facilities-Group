package com.jonesl7l.carinfg.core.network.events

sealed interface SyncEvent {
    data object Connected : SyncEvent

    data class Hint(val changeSeq: Long) : SyncEvent

    data object Unauthorized : SyncEvent
}
