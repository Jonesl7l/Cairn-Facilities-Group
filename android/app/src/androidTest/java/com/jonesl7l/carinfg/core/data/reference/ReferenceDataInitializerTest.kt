package com.jonesl7l.carinfg.core.data.reference

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.EntityTestHelper.referenceEntities
import com.jonesl7l.carinfg.core.database.EntityTestHelper.siteEntity
import com.jonesl7l.carinfg.core.di.NetworkModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReferenceDataInitializerTest {
    private val database: AppDatabase = inMemoryDatabase()
    private val dao = database.referenceDao()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun fillsEmptyTablesFromTheBundledFile() = runTest {
        initializer(dispatcherScheduler = testScheduler).initialize()

        assertThat(dao.observeSites().first()).hasSize(2)
        assertThat(dao.observeTechnicians().first()).hasSize(2)
    }

    @Test
    fun leavesExistingDataAlone() = runTest {
        val existing = siteEntity(id = "site-9", name = "Newer From Server")
        dao.replaceAll(entities = referenceEntities(sites = listOf(existing)))

        initializer(dispatcherScheduler = testScheduler).initialize()

        assertThat(dao.observeSites().first()).containsExactly(existing)
    }

    private fun initializer(dispatcherScheduler: TestCoroutineScheduler) = ReferenceDataInitializer(
        context = ApplicationProvider.getApplicationContext(),
        json = NetworkModule.provideJson(),
        referenceDao = dao,
        ioDispatcher = StandardTestDispatcher(scheduler = dispatcherScheduler),
    )
}
