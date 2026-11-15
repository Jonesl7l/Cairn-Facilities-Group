package com.jonesl7l.carinfg.core.sync.status

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.database.entity.sync.SyncCounts
import com.jonesl7l.carinfg.core.network.connectivity.FakeNetworkMonitor
import com.jonesl7l.carinfg.core.sync.FakeSyncScheduler
import com.jonesl7l.carinfg.core.sync.store.FakeSyncLocalStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncStatusProviderTest {
    private val store = FakeSyncLocalStore()
    private val scheduler = FakeSyncScheduler()
    private val networkMonitor = FakeNetworkMonitor()
    private val provider =
        SyncStatusProvider(
            syncLocalStore = store,
            syncScheduler = scheduler,
            networkMonitor = networkMonitor,
        )

    @Test
    fun `nothing pending is up to date`() = runTest {
        assertThat(indicator()).isEqualTo(SyncIndicator.UpToDate)
    }

    @Test
    fun `pending changes are waiting with their count`() = runTest {
        store.syncCounts.value = counts(pending = 3)

        assertThat(indicator()).isEqualTo(SyncIndicator.Waiting(count = 3))
    }

    @Test
    fun `a running sync is syncing`() = runTest {
        store.syncCounts.value = counts(pending = 3)
        scheduler.isSyncing.value = true

        assertThat(indicator()).isEqualTo(SyncIndicator.Syncing)
    }

    @Test
    fun `needs attention wins over everything else`() = runTest {
        store.syncCounts.value =
            counts(
                pending = 3,
                needsAttention = 1,
            )
        scheduler.isSyncing.value = true

        assertThat(indicator()).isEqualTo(SyncIndicator.NeedsAttention(count = 1))
    }

    @Test
    fun `the status follows connectivity`() = runTest {
        provider.status.test {
            assertThat(awaitItem().isOnline).isEqualTo(true)
            networkMonitor.isOnline.value = false
            assertThat(awaitItem().isOnline).isEqualTo(false)
        }
    }

    private suspend fun indicator(): SyncIndicator = provider.status.first().indicator

    private fun counts(
        pending: Int = 0,
        needsAttention: Int = 0,
    ) = SyncCounts(
        pending = pending,
        needsAttention = needsAttention,
    )
}
