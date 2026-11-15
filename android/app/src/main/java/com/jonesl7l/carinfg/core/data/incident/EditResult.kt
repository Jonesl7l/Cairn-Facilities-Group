package com.jonesl7l.carinfg.core.data.incident

import com.jonesl7l.carinfg.core.domain.validation.FieldError

sealed interface EditResult {
    data object Saved : EditResult

    data class Invalid(val errors: List<FieldError>) : EditResult

    data object NotAllowed : EditResult
}
