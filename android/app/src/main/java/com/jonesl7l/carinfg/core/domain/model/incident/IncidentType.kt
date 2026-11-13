package com.jonesl7l.carinfg.core.domain.model.incident

sealed interface IncidentType {
    data object SlipTripFall : IncidentType

    data object EquipmentFailure : IncidentType

    data object Electrical : IncidentType

    data object FireSmoke : IncidentType

    data object WaterLeak : IncidentType

    data object HazardousSubstance : IncidentType

    data object PropertyDamage : IncidentType

    data object Security : IncidentType

    data object NearMiss : IncidentType

    data class Other(val description: String) : IncidentType

    data class Unknown(val raw: String) : IncidentType
}
