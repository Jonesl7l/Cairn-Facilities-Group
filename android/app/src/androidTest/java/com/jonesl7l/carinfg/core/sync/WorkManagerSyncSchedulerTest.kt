package com.jonesl7l.carinfg.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.jonesl7l.carinfg.core.sync.WorkManagerSyncScheduler.Companion.DELAYED_SYNC_WORK_NAME
import com.jonesl7l.carinfg.core.sync.WorkManagerSyncScheduler.Companion.SYNC_WORK_NAME
import kotlin.time.Duration.Companion.seconds
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkManagerSyncSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scheduler = WorkManagerSyncScheduler(context = context)
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    @Before
    fun setUp() {
        val configuration =
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(SyncRequestWorkerFactory(syncScheduler = scheduler))
                .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
    }

    @Test
    fun aSyncIsEnqueuedWithANetworkConstraint() {
        scheduler.requestSync()

        val work = workInfos(name = SYNC_WORK_NAME).single()
        assertThat(work.state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertThat(work.constraints.requiredNetworkType).isEqualTo(NetworkType.CONNECTED)
    }

    @Test
    fun aSecondRequestKeepsTheQueuedSync() {
        scheduler.requestSync()
        val first = workInfos(name = SYNC_WORK_NAME).single().id

        scheduler.requestSync()

        assertThat(workInfos(name = SYNC_WORK_NAME).single().id).isEqualTo(first)
    }

    @Test
    fun aDelayedRequestWaitsForItsDelay() {
        scheduler.requestSyncAfter(delay = 5.seconds)

        val work = workInfos(name = DELAYED_SYNC_WORK_NAME).single()
        assertThat(work.state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertThat(work.initialDelayMillis).isEqualTo(5.seconds.inWholeMilliseconds)
    }

    @Test
    fun aNewerDelayedRequestReplacesTheOlderOne() {
        scheduler.requestSyncAfter(delay = 5.seconds)
        val first = workInfos(name = DELAYED_SYNC_WORK_NAME).single().id

        scheduler.requestSyncAfter(delay = 10.seconds)

        val current = workInfos(name = DELAYED_SYNC_WORK_NAME).single { it.state == WorkInfo.State.ENQUEUED }
        assertThat(current.id).isNotEqualTo(first)
        assertThat(current.initialDelayMillis).isEqualTo(10.seconds.inWholeMilliseconds)
    }

    @Test
    fun aDelayedRequestAsksForASyncWhenItFires() {
        scheduler.requestSyncAfter(delay = 5.seconds)
        val delayed = workInfos(name = DELAYED_SYNC_WORK_NAME).single().id

        WorkManagerTestInitHelper.getTestDriver(context)?.setInitialDelayMet(delayed)

        assertThat(workInfos(name = DELAYED_SYNC_WORK_NAME).single().state).isEqualTo(WorkInfo.State.SUCCEEDED)
        assertThat(workInfos(name = SYNC_WORK_NAME).single().state).isEqualTo(WorkInfo.State.ENQUEUED)
    }

    @Test
    fun cancelAllCancelsTheSyncAndTheDelayedRequest() {
        scheduler.requestSync()
        scheduler.requestSyncAfter(delay = 5.seconds)

        scheduler.cancelAll()

        assertThat(workInfos(name = SYNC_WORK_NAME).single().state).isEqualTo(WorkInfo.State.CANCELLED)
        assertThat(workInfos(name = DELAYED_SYNC_WORK_NAME).single().state).isEqualTo(WorkInfo.State.CANCELLED)
    }

    private fun workInfos(name: String): List<WorkInfo> = workManager.getWorkInfosForUniqueWork(name).get()

    private class SyncRequestWorkerFactory(private val syncScheduler: SyncScheduler) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker? = if (workerClassName == SyncRequestWorker::class.java.name) {
            SyncRequestWorker(
                context = appContext,
                params = workerParameters,
                syncScheduler = syncScheduler,
            )
        } else {
            null
        }
    }
}
