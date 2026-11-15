package com.jonesl7l.carinfg.core.data.mapper

import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentType
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.model.person.PersonRole
import com.jonesl7l.carinfg.core.domain.model.person.PersonType
import com.jonesl7l.carinfg.core.domain.model.person.Treatment
import com.jonesl7l.carinfg.core.domain.model.user.UserRole

object WireValueMapper {
    private const val OTHER = "OTHER"
    private const val NONE = "NONE"
    private const val SLIP_TRIP_FALL = "SLIP_TRIP_FALL"
    private const val EQUIPMENT_FAILURE = "EQUIPMENT_FAILURE"
    private const val ELECTRICAL = "ELECTRICAL"
    private const val FIRE_SMOKE = "FIRE_SMOKE"
    private const val WATER_LEAK = "WATER_LEAK"
    private const val HAZARDOUS_SUBSTANCE = "HAZARDOUS_SUBSTANCE"
    private const val PROPERTY_DAMAGE = "PROPERTY_DAMAGE"
    private const val SECURITY = "SECURITY"
    private const val NEAR_MISS = "NEAR_MISS"
    private const val LOW = "LOW"
    private const val MEDIUM = "MEDIUM"
    private const val HIGH = "HIGH"
    private const val CRITICAL = "CRITICAL"
    private const val OPEN = "OPEN"
    private const val ASSIGNED = "ASSIGNED"
    private const val IN_PROGRESS = "IN_PROGRESS"
    private const val RESOLVED = "RESOLVED"
    private const val CLOSED = "CLOSED"
    private const val EMPLOYEE = "EMPLOYEE"
    private const val CONTRACTOR = "CONTRACTOR"
    private const val VISITOR = "VISITOR"
    private const val MEMBER_OF_PUBLIC = "MEMBER_OF_PUBLIC"
    private const val INJURED = "INJURED"
    private const val WITNESS = "WITNESS"
    private const val INVOLVED = "INVOLVED"
    private const val FIRST_AID = "FIRST_AID"
    private const val MEDICAL_TREATMENT = "MEDICAL_TREATMENT"
    private const val HOSPITAL = "HOSPITAL"
    private const val AREA_CORDONED_OFF = "AREA_CORDONED_OFF"
    private const val EQUIPMENT_ISOLATED = "EQUIPMENT_ISOLATED"
    private const val FIRST_AID_GIVEN = "FIRST_AID_GIVEN"
    private const val EMERGENCY_SERVICES_CALLED = "EMERGENCY_SERVICES_CALLED"
    private const val SUPERVISOR_NOTIFIED = "SUPERVISOR_NOTIFIED"
    private const val AREA_EVACUATED = "AREA_EVACUATED"
    private const val TECHNICIAN = "TECHNICIAN"
    private const val OPERATOR = "OPERATOR"

    // region IncidentType
    fun toWire(type: IncidentType): String = when (type) {
        IncidentType.SlipTripFall -> SLIP_TRIP_FALL
        IncidentType.EquipmentFailure -> EQUIPMENT_FAILURE
        IncidentType.Electrical -> ELECTRICAL
        IncidentType.FireSmoke -> FIRE_SMOKE
        IncidentType.WaterLeak -> WATER_LEAK
        IncidentType.HazardousSubstance -> HAZARDOUS_SUBSTANCE
        IncidentType.PropertyDamage -> PROPERTY_DAMAGE
        IncidentType.Security -> SECURITY
        IncidentType.NearMiss -> NEAR_MISS
        is IncidentType.Other -> OTHER
        is IncidentType.Unknown -> type.raw
    }

    fun toIncidentType(
        raw: String,
        otherDescription: String?,
    ): IncidentType = when (raw) {
        SLIP_TRIP_FALL -> IncidentType.SlipTripFall
        EQUIPMENT_FAILURE -> IncidentType.EquipmentFailure
        ELECTRICAL -> IncidentType.Electrical
        FIRE_SMOKE -> IncidentType.FireSmoke
        WATER_LEAK -> IncidentType.WaterLeak
        HAZARDOUS_SUBSTANCE -> IncidentType.HazardousSubstance
        PROPERTY_DAMAGE -> IncidentType.PropertyDamage
        SECURITY -> IncidentType.Security
        NEAR_MISS -> IncidentType.NearMiss
        OTHER -> IncidentType.Other(description = otherDescription.orEmpty())
        else -> IncidentType.Unknown(raw = raw)
    }
    // endregion

    // region Severity
    fun toWire(severity: Severity): String = when (severity) {
        Severity.Low -> LOW
        Severity.Medium -> MEDIUM
        Severity.High -> HIGH
        Severity.Critical -> CRITICAL
        is Severity.Unknown -> severity.raw
    }

    fun toSeverity(raw: String): Severity = when (raw) {
        LOW -> Severity.Low
        MEDIUM -> Severity.Medium
        HIGH -> Severity.High
        CRITICAL -> Severity.Critical
        else -> Severity.Unknown(raw = raw)
    }
    // endregion

    // region IncidentStatus
    fun toWire(status: IncidentStatus): String = when (status) {
        IncidentStatus.Open -> OPEN
        IncidentStatus.Assigned -> ASSIGNED
        IncidentStatus.InProgress -> IN_PROGRESS
        IncidentStatus.Resolved -> RESOLVED
        IncidentStatus.Closed -> CLOSED
        is IncidentStatus.Unknown -> status.raw
    }

    fun toIncidentStatus(raw: String): IncidentStatus = when (raw) {
        OPEN -> IncidentStatus.Open
        ASSIGNED -> IncidentStatus.Assigned
        IN_PROGRESS -> IncidentStatus.InProgress
        RESOLVED -> IncidentStatus.Resolved
        CLOSED -> IncidentStatus.Closed
        else -> IncidentStatus.Unknown(raw = raw)
    }
    // endregion

    // region PersonType
    fun toWire(type: PersonType): String = when (type) {
        PersonType.Employee -> EMPLOYEE
        PersonType.Contractor -> CONTRACTOR
        PersonType.Visitor -> VISITOR
        PersonType.MemberOfPublic -> MEMBER_OF_PUBLIC
        is PersonType.Unknown -> type.raw
    }

    fun toPersonType(raw: String): PersonType = when (raw) {
        EMPLOYEE -> PersonType.Employee
        CONTRACTOR -> PersonType.Contractor
        VISITOR -> PersonType.Visitor
        MEMBER_OF_PUBLIC -> PersonType.MemberOfPublic
        else -> PersonType.Unknown(raw = raw)
    }
    // endregion

    // region PersonRole
    fun toWire(role: PersonRole): String = when (role) {
        PersonRole.Injured -> INJURED
        PersonRole.Witness -> WITNESS
        PersonRole.Involved -> INVOLVED
        is PersonRole.Unknown -> role.raw
    }

    fun toPersonRole(raw: String): PersonRole = when (raw) {
        INJURED -> PersonRole.Injured
        WITNESS -> PersonRole.Witness
        INVOLVED -> PersonRole.Involved
        else -> PersonRole.Unknown(raw = raw)
    }
    // endregion

    // region Treatment
    fun toWire(treatment: Treatment): String = when (treatment) {
        Treatment.None -> NONE
        Treatment.FirstAid -> FIRST_AID
        Treatment.MedicalTreatment -> MEDICAL_TREATMENT
        Treatment.Hospital -> HOSPITAL
        is Treatment.Unknown -> treatment.raw
    }

    fun toTreatment(raw: String): Treatment = when (raw) {
        NONE -> Treatment.None
        FIRST_AID -> Treatment.FirstAid
        MEDICAL_TREATMENT -> Treatment.MedicalTreatment
        HOSPITAL -> Treatment.Hospital
        else -> Treatment.Unknown(raw = raw)
    }
    // endregion

    // region ImmediateAction
    fun toWire(action: ImmediateAction): String = when (action) {
        ImmediateAction.None -> NONE
        ImmediateAction.AreaCordonedOff -> AREA_CORDONED_OFF
        ImmediateAction.EquipmentIsolated -> EQUIPMENT_ISOLATED
        ImmediateAction.FirstAidGiven -> FIRST_AID_GIVEN
        ImmediateAction.EmergencyServicesCalled -> EMERGENCY_SERVICES_CALLED
        ImmediateAction.SupervisorNotified -> SUPERVISOR_NOTIFIED
        ImmediateAction.AreaEvacuated -> AREA_EVACUATED
        is ImmediateAction.Other -> OTHER
        is ImmediateAction.Unknown -> action.raw
    }

    fun toImmediateAction(
        raw: String,
        otherDescription: String?,
    ): ImmediateAction = when (raw) {
        NONE -> ImmediateAction.None
        AREA_CORDONED_OFF -> ImmediateAction.AreaCordonedOff
        EQUIPMENT_ISOLATED -> ImmediateAction.EquipmentIsolated
        FIRST_AID_GIVEN -> ImmediateAction.FirstAidGiven
        EMERGENCY_SERVICES_CALLED -> ImmediateAction.EmergencyServicesCalled
        SUPERVISOR_NOTIFIED -> ImmediateAction.SupervisorNotified
        AREA_EVACUATED -> ImmediateAction.AreaEvacuated
        OTHER -> ImmediateAction.Other(description = otherDescription.orEmpty())
        else -> ImmediateAction.Unknown(raw = raw)
    }
    // endregion

    // region UserRole
    fun toWire(role: UserRole): String = when (role) {
        UserRole.Technician -> TECHNICIAN
        UserRole.Operator -> OPERATOR
        is UserRole.Unknown -> role.raw
    }

    fun toUserRole(raw: String): UserRole = when (raw) {
        TECHNICIAN -> UserRole.Technician
        OPERATOR -> UserRole.Operator
        else -> UserRole.Unknown(raw = raw)
    }
    // endregion
}
