package com.jonesl7l.carinfg.core.domain.model.incident

sealed interface IncidentStatus {
    data object Open : IncidentStatus

    data object Assigned : IncidentStatus

    data object InProgress : IncidentStatus

    data object Resolved : IncidentStatus

    data object Closed : IncidentStatus

    data class Unknown(val raw: String) : IncidentStatus
}
