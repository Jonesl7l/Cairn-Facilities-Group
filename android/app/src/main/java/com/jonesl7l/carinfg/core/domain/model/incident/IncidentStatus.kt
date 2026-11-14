package com.jonesl7l.carinfg.core.domain.model.incident

sealed interface IncidentStatus {
    data object Open : IncidentStatus

    data object Assigned : IncidentStatus

    data object InProgress : IncidentStatus

    data object Resolved : IncidentStatus

    data object Closed : IncidentStatus

    /** server value this app version doesnt know yet */
    data class Unknown(val raw: String) : IncidentStatus
}
