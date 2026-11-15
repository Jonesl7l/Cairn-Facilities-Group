package com.jonesl7l.carinfg.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncRequestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncScheduler: SyncScheduler,
) : Worker(context, params) {
    override fun doWork(): Result {
        syncScheduler.requestSync()
        return Result.success()
    }
}
