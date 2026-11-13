package com.jonesl7l.carinfg.core.domain.model.sync

data class SyncStatus(
    val state: SyncState,
    val conflictReason: ConflictReason?,
    val attempts: Int,
    val lastError: String?,
)
