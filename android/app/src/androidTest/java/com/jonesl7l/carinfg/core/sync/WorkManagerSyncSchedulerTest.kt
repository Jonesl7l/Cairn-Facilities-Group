package com.jonesl7l.carinfg.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.sync.WorkManagerSyncScheduler.Companion.SYNC_WORK_NAME
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
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
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
    fun cancelAllCancelsTheSync() {
        scheduler.requestSync()

        scheduler.cancelAll()

        assertThat(workInfos(name = SYNC_WORK_NAME).single().state).isEqualTo(WorkInfo.State.CANCELLED)
    }

    private fun workInfos(name: String): List<WorkInfo> = workManager.getWorkInfosForUniqueWork(name).get()
}
