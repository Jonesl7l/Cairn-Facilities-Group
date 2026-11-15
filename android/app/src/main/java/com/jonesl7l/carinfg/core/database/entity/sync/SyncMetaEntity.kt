package com.jonesl7l.carinfg.core.database.entity.sync

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val lastChangeSeq: Long = 0,
    val databaseId: String? = null,
    val announcedChangeSeq: Long = 0,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
