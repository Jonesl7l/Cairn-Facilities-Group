package com.jonesl7l.carinfg.core.sync

import com.jonesl7l.carinfg.core.data.session.SessionStore
import com.jonesl7l.carinfg.core.sync.pull.PullEngine
import com.jonesl7l.carinfg.core.sync.push.PushEngine
import com.jonesl7l.carinfg.core.sync.store.SyncLocalStore
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import timber.log.Timber

class SyncRunner @Inject constructor(
    private val pushEngine: PushEngine,
    private val pullEngine: PullEngine,
    private val syncLocalStore: SyncLocalStore,
    private val sessionStore: SessionStore,
) {
    suspend fun run(clock: Clock): SyncRunResult {
        sessionStore.session.first() ?: return SyncRunResult.Done
        if (sessionStore.isSessionExpired.first()) {
            Timber.i("run: skipped, the session has expired")
            return SyncRunResult.SessionExpired
        }
        repeat(MAX_PASSES) {
            val result = runPass(now = clock.now())
            if (result == SyncRunResult.SessionExpired) sessionStore.markSessionExpired()
            if (result != SyncRunResult.Done) return result
            if (!hasWorkLeft(now = clock.now())) return SyncRunResult.Done
        }
        Timber.w("run: work left after %d passes, waiting for the next trigger", MAX_PASSES)
        return SyncRunResult.Done
    }

    private suspend fun runPass(now: Instant): SyncRunResult {
        val pushed = pushEngine.push(now = now)
        if (pushed != SyncRunResult.Done) return pushed
        return pullEngine.pull(now = now)
    }

    private suspend fun hasWorkLeft(now: Instant): Boolean {
        val meta = syncLocalStore.getSyncMeta()
        return meta.lastChangeSeq < meta.announcedChangeSeq ||
            syncLocalStore.getPendingIncidents(now = now).isNotEmpty()
    }

    private companion object {
        const val MAX_PASSES = 3
    }
}
