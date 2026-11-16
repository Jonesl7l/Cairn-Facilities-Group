package com.jonesl7l.carinfg.core.domain.policy

import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus

object IncidentPolicyHelper {
    private val editableStatuses = setOf(IncidentStatus.Open, IncidentStatus.Assigned)

    fun canEdit(
        incident: Incident,
        userId: String,
    ): Boolean = incident.isOwnedBy(userId) && incident.status in editableStatuses

    fun canDelete(
        incident: Incident,
        userId: String,
    ): Boolean = incident.isOwnedBy(userId) && incident.status == IncidentStatus.Open

    private fun Incident.isOwnedBy(userId: String) = reporterId == userId

    private val Incident.status get() = operatorFields.status
}
