package com.jonesl7l.carinfg.feature.incident.detail

import com.jonesl7l.carinfg.core.domain.model.incident.IncidentDetail
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState

data class IncidentDetailUiState(
    val isLoading: Boolean = true,
    val detail: IncidentDetail? = null,
    val isMine: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val deleteOutcome: DeleteOutcome? = null,
    val isResolveFailed: Boolean = false,
) {
    val needsAttention: Boolean
        get() = detail?.incident?.syncStatus?.state == SyncState.NEEDS_ATTENTION

    val isLockedDelete: Boolean
        get() = detail?.incident?.serverFields?.deletedAt != null

    val isLocked: Boolean
        get() = isMine && !canEdit && !needsAttention
}
