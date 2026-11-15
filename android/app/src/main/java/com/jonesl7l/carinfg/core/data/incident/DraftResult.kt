package com.jonesl7l.carinfg.core.data.incident

sealed interface DraftResult {
    data object Saved : DraftResult

    data object NotADraft : DraftResult
}
