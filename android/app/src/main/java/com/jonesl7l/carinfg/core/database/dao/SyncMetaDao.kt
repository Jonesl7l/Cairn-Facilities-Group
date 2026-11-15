package com.jonesl7l.carinfg.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetaDao {
    @Query("SELECT * FROM sync_meta LIMIT 1")
    fun observe(): Flow<SyncMetaEntity?>

    @Query("SELECT * FROM sync_meta LIMIT 1")
    suspend fun get(): SyncMetaEntity?

    @Upsert
    suspend fun upsert(meta: SyncMetaEntity)

    // read-then-write in a transaction: API 29's SQLite has no ON CONFLICT DO UPDATE
    @Transaction
    suspend fun raiseAnnouncedChangeSeq(changeSeq: Long) {
        val current = get() ?: SyncMetaEntity.initial
        if (changeSeq > current.announcedChangeSeq) {
            upsert(meta = current.copy(announcedChangeSeq = changeSeq))
        }
    }

    @Query("DELETE FROM sync_meta")
    suspend fun reset()
}
