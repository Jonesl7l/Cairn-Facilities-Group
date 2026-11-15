package com.jonesl7l.carinfg.core.sync.status

sealed interface SyncIndicator {
    data object UpToDate : SyncIndicator

    data object Syncing : SyncIndicator

    data class Waiting(val count: Int) : SyncIndicator

    data class NeedsAttention(val count: Int) : SyncIndicator
}
