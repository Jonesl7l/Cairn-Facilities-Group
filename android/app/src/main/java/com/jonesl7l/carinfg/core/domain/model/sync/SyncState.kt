package com.jonesl7l.carinfg.core.domain.model.sync

enum class SyncState {
    DRAFT,
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
    NEEDS_ATTENTION,
}
