package com.jonesl7l.carinfg.core.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.EntityTestHelper.buildingEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.referenceEntities
import com.jonesl7l.carinfg.core.database.EntityTestHelper.siteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReferenceDaoTest {
    private val database: AppDatabase = inMemoryDatabase()
    private val dao = database.referenceDao()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun isEmptyIsTrueUntilDataIsInserted() = runTest {
        assertThat(dao.isEmpty()).isTrue()

        dao.replaceAll(entities = referenceEntities())

        assertThat(dao.isEmpty()).isFalse()
    }

    @Test
    fun replaceAllRemovesRowsMissingFromTheNewData() = runTest {
        dao.replaceAll(entities = referenceEntities())
        val newSite = siteEntity(
            id = "site-2",
            name = "Depot",
        )
        val newBuilding = buildingEntity(
            id = "building-2",
            siteId = "site-2",
            name = "Warehouse",
        )

        dao.replaceAll(
            entities =
                referenceEntities(
                    sites = listOf(newSite),
                    buildings = listOf(newBuilding),
                ),
        )

        assertThat(dao.observeSites().first()).containsExactly(newSite)
        assertThat(dao.observeBuildings(siteId = "site-1").first()).isEqualTo(emptyList())
        assertThat(dao.observeBuildings(siteId = "site-2").first()).containsExactly(newBuilding)
    }

    @Test
    fun insertAllIfEmptyLeavesExistingDataAlone() = runTest {
        val existing = siteEntity(
            id = "site-9",
            name = "Newer From Server",
        )
        dao.replaceAll(entities = referenceEntities(sites = listOf(existing)))

        dao.insertAllIfEmpty(entities = referenceEntities())

        assertThat(dao.observeSites().first()).containsExactly(existing)
    }

    @Test
    fun childListsAreFilteredByParentAndSortedByName() = runTest {
        val zebra = buildingEntity(
            id = "building-2",
            name = "Zebra House",
        )
        val alpha = buildingEntity(
            id = "building-3",
            name = "Alpha House",
        )
        val elsewhere = buildingEntity(
            id = "building-4",
            siteId = "site-2",
            name = "Elsewhere",
        )

        dao.replaceAll(entities = referenceEntities(buildings = listOf(zebra, alpha, elsewhere)))

        assertThat(dao.observeBuildings(siteId = "site-1").first()).containsExactly(alpha, zebra)
    }
}
