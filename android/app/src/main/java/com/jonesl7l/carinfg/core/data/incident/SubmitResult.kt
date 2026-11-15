package com.jonesl7l.carinfg.core.data.incident

import com.jonesl7l.carinfg.core.domain.validation.FieldError

sealed interface SubmitResult {
    data object Submitted : SubmitResult

    data class Invalid(val errors: List<FieldError>) : SubmitResult

    data object NotADraft : SubmitResult
}
