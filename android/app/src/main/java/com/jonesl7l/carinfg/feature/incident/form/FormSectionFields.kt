package com.jonesl7l.carinfg.feature.incident.form

import com.jonesl7l.carinfg.core.domain.validation.IncidentField

val detailsFields =
    setOf(
        IncidentField.Title,
        IncidentField.Description,
        IncidentField.OccurredAt,
        IncidentField.Type,
        IncidentField.Severity,
    )

val locationFields =
    setOf(
        IncidentField.Site,
        IncidentField.Building,
        IncidentField.EquipmentOutOfService,
    )

val safetyFields =
    setOf(
        IncidentField.InjuryOccurred,
        IncidentField.HazardStillPresent,
        IncidentField.ImmediateActions,
    )
