package com.jonesl7l.carinfg.core.data.incident

import com.jonesl7l.carinfg.core.domain.validation.FieldError

sealed interface SaveResult {
    data object Saved : SaveResult

    data class Invalid(val errors: List<FieldError>) : SaveResult

    data object NotAllowed : SaveResult
}
