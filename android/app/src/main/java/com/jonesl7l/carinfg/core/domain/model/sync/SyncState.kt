package com.jonesl7l.carinfg.core.domain.model.sync

enum class SyncState {
    /** local only, not queued for sync */
    DRAFT,
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,

    /** sync stopped, conflictReason says why */
    NEEDS_ATTENTION,
}
