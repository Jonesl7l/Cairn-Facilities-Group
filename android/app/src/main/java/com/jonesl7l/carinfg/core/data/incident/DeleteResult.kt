package com.jonesl7l.carinfg.core.data.incident

import kotlin.time.Instant

sealed interface DeleteResult {
    data object Removed : DeleteResult

    data class PendingDelete(val undoDeadline: Instant) : DeleteResult

    data object NotAllowed : DeleteResult
}
