package com.jonesl7l.carinfg.core.domain.model.incident

sealed interface Severity {
    data object Low : Severity

    data object Medium : Severity

    data object High : Severity

    data object Critical : Severity

    data class Unknown(val raw: String) : Severity
}
