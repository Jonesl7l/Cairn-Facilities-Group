package com.jonesl7l.carinfg.core.database.entity.sync

data class SyncCounts(
    val pending: Int,
    val needsAttention: Int,
) {
    companion object {
        val none =
            SyncCounts(
                pending = 0,
                needsAttention = 0,
            )
    }
}
