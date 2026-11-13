package com.jonesl7l.carinfg.core.domain.validation

sealed interface IncidentField {
    data object Title : IncidentField

    data object Description : IncidentField

    data object OccurredAt : IncidentField

    data object Type : IncidentField

    data object Severity : IncidentField

    data object Site : IncidentField

    data object Building : IncidentField

    data object EquipmentOutOfService : IncidentField

    data object InjuryOccurred : IncidentField

    data object HazardStillPresent : IncidentField

    data object ImmediateActions : IncidentField

    data object Photos : IncidentField

    data class Person(
        val index: Int,
        val field: PersonField,
    ) : IncidentField
}
