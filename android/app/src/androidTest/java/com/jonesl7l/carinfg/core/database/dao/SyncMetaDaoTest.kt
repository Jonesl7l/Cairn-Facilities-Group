package com.jonesl7l.carinfg.core.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.EntityTestHelper.syncMetaEntity
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncMetaDaoTest {
    private val database: AppDatabase = inMemoryDatabase()
    private val dao = database.syncMetaDao()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun isNullBeforeTheFirstWrite() = runTest {
        assertThat(dao.get()).isNull()
    }

    @Test
    fun upsertRoundTripsAndReplacesTheSingleRow() = runTest {
        dao.upsert(meta = syncMetaEntity())
        val next = syncMetaEntity(
            lastChangeSeq = 92,
            databaseId = "db-2",
        )

        dao.upsert(meta = next)

        assertThat(dao.get()).isEqualTo(next)
    }

    @Test
    fun raiseAnnouncedChangeSeqStartsFromTheInitialRowWhenEmpty() = runTest {
        dao.raiseAnnouncedChangeSeq(changeSeq = 5)

        assertThat(dao.get()).isEqualTo(SyncMetaEntity(announcedChangeSeq = 5))
    }

    @Test
    fun raiseAnnouncedChangeSeqOnlyEverIncreasesAndKeepsTheCursor() = runTest {
        dao.upsert(
            meta = syncMetaEntity(
                lastChangeSeq = 10,
                announcedChangeSeq = 20,
            ),
        )

        dao.raiseAnnouncedChangeSeq(changeSeq = 15)
        assertThat(dao.get()).isEqualTo(
            syncMetaEntity(
                lastChangeSeq = 10,
                announcedChangeSeq = 20,
            ),
        )

        dao.raiseAnnouncedChangeSeq(changeSeq = 30)
        assertThat(dao.get()).isEqualTo(
            syncMetaEntity(
                lastChangeSeq = 10,
                announcedChangeSeq = 30,
            ),
        )
    }

    @Test
    fun resetClearsTheRow() = runTest {
        dao.upsert(meta = syncMetaEntity())

        dao.reset()

        assertThat(dao.get()).isNull()
    }
}
