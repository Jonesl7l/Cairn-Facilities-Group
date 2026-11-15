package com.jonesl7l.carinfg.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class WorkManagerSyncScheduler @Inject constructor(@ApplicationContext private val context: Context) : SyncScheduler {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    override fun requestSync() {
        val request =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraints)
                .setBackoffCriteria(
                    backoffPolicy = BackoffPolicy.EXPONENTIAL,
                    duration = backoffDelay.toJavaDuration(),
                ).build()
        workManager.enqueueUniqueWork(
            uniqueWorkName = SYNC_WORK_NAME,
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = request,
        )
    }

    override fun requestSyncAfter(delay: Duration) {
        val request =
            OneTimeWorkRequestBuilder<SyncRequestWorker>()
                .setInitialDelay(delay.toJavaDuration())
                .build()
        workManager.enqueueUniqueWork(
            uniqueWorkName = DELAYED_SYNC_WORK_NAME,
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            request = request,
        )
    }

    override fun cancelAll() {
        workManager.cancelUniqueWork(uniqueWorkName = DELAYED_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(uniqueWorkName = SYNC_WORK_NAME)
    }

    companion object {
        const val SYNC_WORK_NAME = "sync"
        const val DELAYED_SYNC_WORK_NAME = "delayed_sync"

        private val backoffDelay = 30.seconds
        private val networkConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    }
}
