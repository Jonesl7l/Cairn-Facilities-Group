package com.jonesl7l.carinfg.core.sync.pull

import com.jonesl7l.carinfg.core.data.session.SessionStore
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import com.jonesl7l.carinfg.core.network.datasource.IncidentChangesPage
import com.jonesl7l.carinfg.core.network.datasource.IncidentRemoteDataSource
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import com.jonesl7l.carinfg.core.network.result.ErrorCategory
import com.jonesl7l.carinfg.core.sync.SyncRunResult
import com.jonesl7l.carinfg.core.sync.store.SyncLocalStore
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import timber.log.Timber

class PullEngine @Inject constructor(
    private val incidentRemoteDataSource: IncidentRemoteDataSource,
    private val syncLocalStore: SyncLocalStore,
    private val sessionStore: SessionStore,
) {
    suspend fun pull(now: Instant): SyncRunResult {
        val userId = sessionStore.session.first()?.id ?: return SyncRunResult.Done
        while (true) {
            val meta = syncLocalStore.getSyncMeta()
            val page =
                when (val result = incidentRemoteDataSource.getChanges(afterChangeSeq = meta.lastChangeSeq)) {
                    is ApiResult.Failure -> return result.error.toRunResult()
                    is ApiResult.Success -> result.value
                }
            if (meta.databaseId != null && meta.databaseId != page.databaseId) {
                Timber.i("pull: server database changed from %s to %s, resyncing", meta.databaseId, page.databaseId)
                syncLocalStore.resetForNewDatabase(databaseId = page.databaseId)
                continue
            }
            applyPage(
                page = page,
                meta = meta,
                userId = userId,
                now = now,
            )
            if (page.isLastPage) break
        }
        syncLocalStore.pruneOutOfScope(
            userId = userId,
            updatedBefore = now - PullRuleHelper.scopeWindow,
        )
        return SyncRunResult.Done
    }

    private suspend fun applyPage(
        page: IncidentChangesPage,
        meta: SyncMetaEntity,
        userId: String,
        now: Instant,
    ) {
        val locals = syncLocalStore.getIncidents(ids = page.incidents.map { it.id })
        val actions =
            page.incidents.map { remote ->
                PullRuleHelper.decide(
                    local = locals[remote.id],
                    remote = remote,
                    userId = userId,
                    now = now,
                )
            }
        val cursor = page.incidents.maxOfOrNull { it.changeSeq } ?: meta.lastChangeSeq
        syncLocalStore.applyPullPage(
            actions = actions,
            meta =
                meta.copy(
                    lastChangeSeq = cursor,
                    databaseId = page.databaseId,
                ),
        )
    }

    private fun ApiError.toRunResult(): SyncRunResult = when {
        this == ApiError.Unauthenticated -> SyncRunResult.SessionExpired
        category == ErrorCategory.RETRY -> SyncRunResult.RetryWithBackoff
        else -> SyncRunResult.Done
    }
}
