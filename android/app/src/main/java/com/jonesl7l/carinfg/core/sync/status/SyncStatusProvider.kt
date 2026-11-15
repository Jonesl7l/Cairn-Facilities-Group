package com.jonesl7l.carinfg.core.sync.status

import com.jonesl7l.carinfg.core.database.entity.sync.SyncCounts
import com.jonesl7l.carinfg.core.network.connectivity.NetworkMonitor
import com.jonesl7l.carinfg.core.sync.SyncScheduler
import com.jonesl7l.carinfg.core.sync.store.SyncLocalStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class SyncStatusProvider @Inject constructor(
    syncLocalStore: SyncLocalStore,
    syncScheduler: SyncScheduler,
    networkMonitor: NetworkMonitor,
) {
    val status: Flow<AppSyncStatus> =
        combine(
            syncLocalStore.observeSyncCounts(),
            syncScheduler.isSyncing,
            networkMonitor.isOnline,
        ) { counts, isSyncing, isOnline ->
            AppSyncStatus(
                isOnline = isOnline,
                indicator =
                    indicator(
                        counts = counts,
                        isSyncing = isSyncing,
                    ),
            )
        }.distinctUntilChanged()

    private fun indicator(
        counts: SyncCounts,
        isSyncing: Boolean,
    ): SyncIndicator = when {
        counts.needsAttention > 0 -> SyncIndicator.NeedsAttention(count = counts.needsAttention)
        isSyncing -> SyncIndicator.Syncing
        counts.pending > 0 -> SyncIndicator.Waiting(count = counts.pending)
        else -> SyncIndicator.UpToDate
    }
}
