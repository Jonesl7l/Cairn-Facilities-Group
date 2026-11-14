package com.jonesl7l.carinfg.core.domain.model.incident

import kotlinx.datetime.LocalDate

/** set by operators on the dashboard, read only in the app */
data class OperatorFields(
    val status: IncidentStatus,
    val assignedTo: String?,
    val dueDate: LocalDate?,
    val rootCause: String?,
    val correctiveActions: String?,
    val regulatoryReportable: Boolean?,
    val supervisorAcknowledged: Boolean?,
)
