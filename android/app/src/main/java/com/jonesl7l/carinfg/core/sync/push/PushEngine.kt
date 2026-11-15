package com.jonesl7l.carinfg.core.sync.push

import com.jonesl7l.carinfg.core.data.mapper.toDeleteRequest
import com.jonesl7l.carinfg.core.data.mapper.toDomain
import com.jonesl7l.carinfg.core.data.mapper.toWriteRequest
import com.jonesl7l.carinfg.core.data.session.SessionStore
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.domain.model.photo.PhotoUploadState
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.policy.IncidentPolicyHelper
import com.jonesl7l.carinfg.core.network.datasource.IncidentRemoteDataSource
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import com.jonesl7l.carinfg.core.sync.SyncRunResult
import com.jonesl7l.carinfg.core.sync.store.SyncLocalStore
import java.io.File
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import timber.log.Timber

class PushEngine @Inject constructor(
    private val incidentRemoteDataSource: IncidentRemoteDataSource,
    private val syncLocalStore: SyncLocalStore,
    private val sessionStore: SessionStore,
) {
    suspend fun push(now: Instant): SyncRunResult {
        val userId = sessionStore.session.first()?.id ?: return SyncRunResult.Done
        for (local in syncLocalStore.getPendingIncidents(now = now)) {
            val outcome = pushIncident(
                local = local,
                userId = userId,
            )
            if (outcome is IncidentPushOutcome.Stop) return outcome.result
        }
        return SyncRunResult.Done
    }

    private suspend fun pushIncident(
        local: IncidentWithPhotos,
        userId: String,
    ): IncidentPushOutcome {
        val hasPendingPhotos = local.photos.any { it.uploadState == PhotoUploadState.PENDING }
        if (local.incident.syncState == SyncState.PENDING_DELETE || !hasPendingPhotos) {
            return send(
                local = local,
                userId = userId,
            )
        }
        val uploaded = uploadPendingPhotos(
            local = local,
            userId = userId,
        )
        if (uploaded != IncidentPushOutcome.Continue) return uploaded
        val refreshed = syncLocalStore.getIncidents(ids = listOf(local.incident.id))[local.incident.id]
        return refreshed?.let {
            send(
                local = it,
                userId = userId,
            )
        } ?: IncidentPushOutcome.Done
    }

    // region uploadPendingPhotos
    private suspend fun uploadPendingPhotos(
        local: IncidentWithPhotos,
        userId: String,
    ): IncidentPushOutcome {
        val canAddPhotos =
            IncidentPolicyHelper.canAddPhotos(
                incident = local.toDomain(),
                userId = userId,
            )
        if (!canAddPhotos) {
            return needsAttention(
                local = local,
                reason = ConflictReason.STATUS_LOCKED,
                error = STATUS_LOCKED_ERROR,
            )
        }
        val pending = local.photos.filter { it.uploadState == PhotoUploadState.PENDING }.sortedBy { it.position }
        for (photo in pending) {
            val path = photo.localPath
            if (path == null) {
                Timber.w("uploadPendingPhotos: photo %s has no local file", photo.id)
                continue
            }
            val result =
                incidentRemoteDataSource.uploadPhoto(
                    incidentId = local.incident.id,
                    photoId = photo.id,
                    file = File(path),
                )
            when (result) {
                is ApiResult.Success -> syncLocalStore.markPhotoUploaded(
                    photoId = photo.id,
                    remoteUrl = result.value.url,
                )

                is ApiResult.Failure -> return handleError(
                    error = result.error,
                    local = local,
                    operation = PushOperation.PHOTO_UPLOAD,
                )
            }
        }
        return IncidentPushOutcome.Continue
    }

    // endregion

    // region send
    private suspend fun send(
        local: IncidentWithPhotos,
        userId: String,
    ): IncidentPushOutcome = when (local.incident.syncState) {
        SyncState.PENDING_CREATE -> sendCreate(local = local)

        SyncState.PENDING_UPDATE -> sendUpdate(
            local = local,
            userId = userId,
        )

        SyncState.PENDING_DELETE -> sendDelete(
            local = local,
            userId = userId,
        )

        else -> IncidentPushOutcome.Done
    }

    private suspend fun sendCreate(local: IncidentWithPhotos): IncidentPushOutcome {
        val request = local.toWriteRequest() ?: return missingRequiredFields(local = local)
        return handleResult(
            result = incidentRemoteDataSource.createIncident(request = request),
            local = local,
            operation = PushOperation.CREATE,
        )
    }

    private suspend fun sendUpdate(
        local: IncidentWithPhotos,
        userId: String,
    ): IncidentPushOutcome {
        val row = local.incident
        val canEdit =
            IncidentPolicyHelper.canEdit(
                incident = local.toDomain(),
                userId = userId,
            )
        if (!canEdit) {
            return needsAttention(
                local = local,
                reason = ConflictReason.STATUS_LOCKED,
                error = STATUS_LOCKED_ERROR,
            )
        }
        val version = row.serverVersion ?: return sendCreate(local = local)
        val request = local.toWriteRequest() ?: return missingRequiredFields(local = local)
        val result =
            sendWithVersionRetry(
                id = row.id,
                version = version,
            ) { currentVersion ->
                incidentRemoteDataSource.updateIncident(
                    id = row.id,
                    version = currentVersion,
                    request = request,
                )
            }
        return handleResult(
            result = result,
            local = local,
            operation = PushOperation.UPDATE,
        )
    }

    private suspend fun sendDelete(
        local: IncidentWithPhotos,
        userId: String,
    ): IncidentPushOutcome {
        val row = local.incident
        val version = row.serverVersion ?: return removeLocally(local = local)
        val canDelete =
            IncidentPolicyHelper.canDelete(
                incident = local.toDomain(),
                userId = userId,
            )
        if (!canDelete) {
            return needsAttention(
                local = local,
                reason = ConflictReason.STATUS_LOCKED,
                error = STATUS_LOCKED_ERROR,
            )
        }
        val request = row.toDeleteRequest()
        if (request == null) {
            Timber.e("sendDelete: pending delete %s has no deletedAt or deletedBy", row.id)
            syncLocalStore.recordFailure(
                id = row.id,
                error = MISSING_DELETE_FIELDS_ERROR,
            )
            return IncidentPushOutcome.Done
        }
        val result =
            sendWithVersionRetry(
                id = row.id,
                version = version,
            ) { currentVersion ->
                incidentRemoteDataSource.deleteIncident(
                    id = row.id,
                    version = currentVersion,
                    request = request,
                )
            }
        return handleResult(
            result = result,
            local = local,
            operation = PushOperation.DELETE,
        )
    }

    private suspend fun sendWithVersionRetry(
        id: String,
        version: Int,
        send: suspend (Int) -> ApiResult<IncidentDto>,
    ): ApiResult<IncidentDto> {
        val first = send(version)
        val conflict = (first as? ApiResult.Failure)?.error as? ApiError.VersionConflict ?: return first
        val current =
            conflict.current ?: when (val fetched = incidentRemoteDataSource.getIncident(id = id)) {
                is ApiResult.Success -> fetched.value
                is ApiResult.Failure -> return fetched
            }
        return send(current.version)
    }

    // endregion

    // region handleResult
    private suspend fun handleResult(
        result: ApiResult<IncidentDto>,
        local: IncidentWithPhotos,
        operation: PushOperation,
    ): IncidentPushOutcome = when (result) {
        is ApiResult.Failure -> handleError(
            error = result.error,
            local = local,
            operation = operation,
        )

        is ApiResult.Success -> if (operation == PushOperation.DELETE) {
            removeLocally(local = local)
        } else {
            commit(
                local = local,
                remote = result.value,
            )
        }
    }

    private suspend fun commit(
        local: IncidentWithPhotos,
        remote: IncidentDto,
    ): IncidentPushOutcome {
        val basedOnRevision = local.incident.localRevision
        syncLocalStore.commitPush(id = local.incident.id) { current ->
            PushCommitHelper.commit(
                current = current,
                remote = remote,
                basedOnRevision = basedOnRevision,
            )
        }
        return IncidentPushOutcome.Done
    }

    private suspend fun handleError(
        error: ApiError,
        local: IncidentWithPhotos,
        operation: PushOperation,
    ): IncidentPushOutcome = when (
        val action = PushErrorHelper.decide(
            error = error,
            operation = operation,
        )
    ) {
        is PushErrorAction.NeedsAttention -> needsAttention(
            local = local,
            reason = action.reason,
            error = error.name,
        )

        PushErrorAction.RemoveLocally -> removeLocally(local = local)

        PushErrorAction.ResubmitAsCreate -> sendCreate(local = local)

        PushErrorAction.LeaveForNextPass -> IncidentPushOutcome.Done

        PushErrorAction.RecordFailure -> {
            syncLocalStore.recordFailure(
                id = local.incident.id,
                error = error.name,
            )
            IncidentPushOutcome.Done
        }

        PushErrorAction.StopForSession -> IncidentPushOutcome.Stop(result = SyncRunResult.SessionExpired)

        PushErrorAction.StopAndRetry -> {
            syncLocalStore.recordFailure(
                id = local.incident.id,
                error = error.name,
            )
            IncidentPushOutcome.Stop(result = SyncRunResult.RetryWithBackoff)
        }
    }

    private suspend fun needsAttention(
        local: IncidentWithPhotos,
        reason: ConflictReason,
        error: String,
    ): IncidentPushOutcome {
        syncLocalStore.markNeedsAttention(
            id = local.incident.id,
            reason = reason,
            error = error,
            basedOnRevision = local.incident.localRevision,
        )
        return IncidentPushOutcome.Done
    }

    private suspend fun missingRequiredFields(local: IncidentWithPhotos): IncidentPushOutcome {
        Timber.w("missingRequiredFields: %s is missing a required field", local.incident.id)
        return needsAttention(
            local = local,
            reason = ConflictReason.VALIDATION,
            error = MISSING_FIELDS_ERROR,
        )
    }

    private suspend fun removeLocally(local: IncidentWithPhotos): IncidentPushOutcome {
        syncLocalStore.removePushed(
            id = local.incident.id,
            basedOnRevision = local.incident.localRevision,
        )
        return IncidentPushOutcome.Done
    }

    private val ApiError.name: String
        get() = this::class.simpleName.orEmpty()
    // endregion

    private companion object {
        const val STATUS_LOCKED_ERROR = "StatusLocked"
        const val MISSING_FIELDS_ERROR = "MissingRequiredFields"
        const val MISSING_DELETE_FIELDS_ERROR = "MissingDeleteFields"
    }
}
