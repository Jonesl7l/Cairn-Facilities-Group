package com.jonesl7l.carinfg.feature.incident

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentType
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.model.person.PersonRole
import com.jonesl7l.carinfg.core.domain.model.person.PersonType
import com.jonesl7l.carinfg.core.domain.model.person.Treatment

@Composable
fun statusLabel(status: IncidentStatus): String = stringResource(
    id =
        when (status) {
            IncidentStatus.Open -> R.string.status_open
            IncidentStatus.Assigned -> R.string.status_assigned
            IncidentStatus.InProgress -> R.string.status_in_progress
            IncidentStatus.Resolved -> R.string.status_resolved
            IncidentStatus.Closed -> R.string.status_closed
            is IncidentStatus.Unknown -> R.string.status_unknown
        },
)

@Composable
fun severityLabel(severity: Severity): String = stringResource(
    id =
        when (severity) {
            Severity.Low -> R.string.severity_low
            Severity.Medium -> R.string.severity_medium
            Severity.High -> R.string.severity_high
            Severity.Critical -> R.string.severity_critical
            is Severity.Unknown -> R.string.severity_unknown
        },
)

@Composable
fun typeLabel(type: IncidentType): String = when (type) {
    is IncidentType.Other -> type.description

    else -> stringResource(
        id =
            when (type) {
                IncidentType.SlipTripFall -> R.string.type_slip_trip_fall
                IncidentType.EquipmentFailure -> R.string.type_equipment_failure
                IncidentType.Electrical -> R.string.type_electrical
                IncidentType.FireSmoke -> R.string.type_fire_smoke
                IncidentType.WaterLeak -> R.string.type_water_leak
                IncidentType.HazardousSubstance -> R.string.type_hazardous_substance
                IncidentType.PropertyDamage -> R.string.type_property_damage
                IncidentType.Security -> R.string.type_security
                IncidentType.NearMiss -> R.string.type_near_miss
                else -> R.string.type_unknown
            },
    )
}

@Composable
fun roleLabel(role: PersonRole): String = stringResource(
    id =
        when (role) {
            PersonRole.Injured -> R.string.role_injured
            PersonRole.Witness -> R.string.role_witness
            PersonRole.Involved -> R.string.role_involved
            is PersonRole.Unknown -> R.string.role_unknown
        },
)

@Composable
fun personTypeLabel(type: PersonType): String = stringResource(
    id =
        when (type) {
            PersonType.Employee -> R.string.person_type_employee
            PersonType.Contractor -> R.string.person_type_contractor
            PersonType.Visitor -> R.string.person_type_visitor
            PersonType.MemberOfPublic -> R.string.person_type_member_of_public
            is PersonType.Unknown -> R.string.value_unknown
        },
)

@Composable
fun treatmentLabel(treatment: Treatment): String = stringResource(
    id =
        when (treatment) {
            Treatment.None -> R.string.treatment_none
            Treatment.FirstAid -> R.string.treatment_first_aid
            Treatment.MedicalTreatment -> R.string.treatment_medical
            Treatment.Hospital -> R.string.treatment_hospital
            is Treatment.Unknown -> R.string.value_unknown
        },
)

@Composable
fun actionLabel(action: ImmediateAction): String = stringResource(
    id =
        when (action) {
            ImmediateAction.None -> R.string.action_none
            ImmediateAction.AreaCordonedOff -> R.string.action_area_cordoned_off
            ImmediateAction.EquipmentIsolated -> R.string.action_equipment_isolated
            ImmediateAction.FirstAidGiven -> R.string.action_first_aid_given
            ImmediateAction.EmergencyServicesCalled -> R.string.action_emergency_services_called
            ImmediateAction.SupervisorNotified -> R.string.action_supervisor_notified
            ImmediateAction.AreaEvacuated -> R.string.action_area_evacuated
            is ImmediateAction.Other -> R.string.action_other
            is ImmediateAction.Unknown -> R.string.value_unknown
        },
)
