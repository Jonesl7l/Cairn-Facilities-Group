package com.jonesl7l.carinfg.core.sync.status

data class AppSyncStatus(
    val isOnline: Boolean,
    val indicator: SyncIndicator,
)
