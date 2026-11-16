package com.jonesl7l.carinfg.core.data.reference

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.database.EntityTestHelper.buildingEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.referenceEntities
import com.jonesl7l.carinfg.core.database.EntityTestHelper.siteEntity
import com.jonesl7l.carinfg.core.domain.model.reference.Building
import com.jonesl7l.carinfg.core.domain.model.reference.Site
import com.jonesl7l.carinfg.core.network.datasource.FakeReferenceRemoteDataSource
import com.jonesl7l.carinfg.core.network.dto.reference.ReferenceDataDto
import com.jonesl7l.carinfg.core.network.dto.reference.SiteDto
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReferenceRepositoryTest {
    private val database: AppDatabase = inMemoryDatabase()
    private val dao = database.referenceDao()
    private val remote = FakeReferenceRemoteDataSource(result = ApiResult.Success(value = serverData))

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun listsAreSortedAndMappedToTheDomain() = runTest {
        val zebra = buildingEntity(
            id = "building-2",
            name = "Zebra House",
        )
        val alpha = buildingEntity(
            id = "building-3",
            name = "Alpha House",
        )
        dao.replaceAll(entities = referenceEntities(buildings = listOf(zebra, alpha)))

        val buildings = repository(scope = backgroundScope).observeBuildings().first()

        assertThat(buildings).containsExactly(
            Building(
                id = "building-3",
                siteId = "site-1",
                name = "Alpha House",
            ),
            Building(
                id = "building-2",
                siteId = "site-1",
                name = "Zebra House",
            ),
        )
    }

    @Test
    fun aSuccessfulRefreshReplacesTheData() = runTest {
        dao.replaceAll(
            entities = referenceEntities(
                sites = listOf(
                    siteEntity(
                        id = "site-old",
                        name = "Old",
                    ),
                ),
            ),
        )
        val repository = repository(scope = backgroundScope)

        repository.refresh().join()

        assertThat(repository.observeSites().first()).containsExactly(
            Site(
                id = "site-new",
                name = "New Campus",
            ),
        )
    }

    @Test
    fun aFailedRefreshKeepsTheOldData() = runTest {
        val old = siteEntity(
            id = "site-old",
            name = "Old",
        )
        dao.replaceAll(entities = referenceEntities(sites = listOf(old)))
        remote.result = ApiResult.Failure(error = ApiError.ServiceUnavailable(retryAfter = null))
        val repository = repository(scope = backgroundScope)

        repository.refresh().join()

        assertThat(repository.observeSites().first()).containsExactly(
            Site(
                id = "site-old",
                name = "Old",
            ),
        )
    }

    @Test
    fun aRefreshInProgressIsReusedInsteadOfStartingAnother() = runTest {
        val gate = CompletableDeferred<Unit>()
        remote.gate = gate
        val repository = repository(scope = backgroundScope)

        val first = repository.refresh()
        val second = repository.refresh()
        gate.complete(Unit)
        first.join()

        assertThat(second).isSameInstanceAs(first)
        assertThat(remote.calls).isEqualTo(1)
    }

    private fun repository(scope: CoroutineScope) = ReferenceRepository(
        referenceDao = dao,
        referenceRemoteDataSource = remote,
        applicationScope = scope,
    )

    private companion object {
        val serverData =
            ReferenceDataDto(
                sites = listOf(
                    SiteDto(
                        id = "site-new",
                        name = "New Campus",
                    ),
                ),
                buildings = emptyList(),
                floors = emptyList(),
                rooms = emptyList(),
                assets = emptyList(),
                technicians = emptyList(),
            )
    }
}
