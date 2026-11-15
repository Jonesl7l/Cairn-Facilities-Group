package com.jonesl7l.carinfg.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.time.Clock

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRunner: SyncRunner,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (syncRunner.run(clock = Clock.System)) {
        SyncRunResult.RetryWithBackoff -> Result.retry()
        SyncRunResult.Done, SyncRunResult.SessionExpired -> Result.success()
    }
}
