package com.jonesl7l.carinfg.core.data.mapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentType
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.model.person.PersonRole
import com.jonesl7l.carinfg.core.domain.model.person.PersonType
import com.jonesl7l.carinfg.core.domain.model.person.Treatment
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import org.junit.Test

class WireValueMapperTest {
    @Test
    fun `incident types map both ways`() {
        val cases =
            listOf(
                IncidentType.SlipTripFall to "SLIP_TRIP_FALL",
                IncidentType.EquipmentFailure to "EQUIPMENT_FAILURE",
                IncidentType.Electrical to "ELECTRICAL",
                IncidentType.FireSmoke to "FIRE_SMOKE",
                IncidentType.WaterLeak to "WATER_LEAK",
                IncidentType.HazardousSubstance to "HAZARDOUS_SUBSTANCE",
                IncidentType.PropertyDamage to "PROPERTY_DAMAGE",
                IncidentType.Security to "SECURITY",
                IncidentType.NearMiss to "NEAR_MISS",
                IncidentType.Other(description = "Pigeon in the lift") to "OTHER",
                IncidentType.Unknown(raw = "FLOOD") to "FLOOD",
            )

        cases.forEach { (type, wire) ->
            assertThat(WireValueMapper.toWire(type = type)).isEqualTo(wire)
            val otherDescription = (type as? IncidentType.Other)?.description
            assertThat(
                WireValueMapper.toIncidentType(
                    raw = wire,
                    otherDescription = otherDescription,
                ),
            ).isEqualTo(type)
        }
    }

    @Test
    fun `other without its text becomes other with empty text`() {
        assertThat(
            WireValueMapper.toIncidentType(
                raw = "OTHER",
                otherDescription = null,
            ),
        )
            .isEqualTo(IncidentType.Other(description = ""))
        assertThat(
            WireValueMapper.toImmediateAction(
                raw = "OTHER",
                otherDescription = null,
            ),
        )
            .isEqualTo(ImmediateAction.Other(description = ""))
    }

    @Test
    fun `severities map both ways`() {
        val cases =
            listOf(
                Severity.Low to "LOW",
                Severity.Medium to "MEDIUM",
                Severity.High to "HIGH",
                Severity.Critical to "CRITICAL",
                Severity.Unknown(raw = "SEVERE") to "SEVERE",
            )

        cases.forEach { (severity, wire) ->
            assertThat(WireValueMapper.toWire(severity = severity)).isEqualTo(wire)
            assertThat(WireValueMapper.toSeverity(raw = wire)).isEqualTo(severity)
        }
    }

    @Test
    fun `statuses map both ways`() {
        val cases =
            listOf(
                IncidentStatus.Open to "OPEN",
                IncidentStatus.Assigned to "ASSIGNED",
                IncidentStatus.InProgress to "IN_PROGRESS",
                IncidentStatus.Resolved to "RESOLVED",
                IncidentStatus.Closed to "CLOSED",
                IncidentStatus.Unknown(raw = "ON_HOLD") to "ON_HOLD",
            )

        cases.forEach { (status, wire) ->
            assertThat(WireValueMapper.toWire(status = status)).isEqualTo(wire)
            assertThat(WireValueMapper.toIncidentStatus(raw = wire)).isEqualTo(status)
        }
    }

    @Test
    fun `person types and roles map both ways`() {
        val types =
            listOf(
                PersonType.Employee to "EMPLOYEE",
                PersonType.Contractor to "CONTRACTOR",
                PersonType.Visitor to "VISITOR",
                PersonType.MemberOfPublic to "MEMBER_OF_PUBLIC",
                PersonType.Unknown(raw = "VOLUNTEER") to "VOLUNTEER",
            )
        val roles =
            listOf(
                PersonRole.Injured to "INJURED",
                PersonRole.Witness to "WITNESS",
                PersonRole.Involved to "INVOLVED",
                PersonRole.Unknown(raw = "FIRST_AIDER") to "FIRST_AIDER",
            )

        types.forEach { (type, wire) ->
            assertThat(WireValueMapper.toWire(type = type)).isEqualTo(wire)
            assertThat(WireValueMapper.toPersonType(raw = wire)).isEqualTo(type)
        }
        roles.forEach { (role, wire) ->
            assertThat(WireValueMapper.toWire(role = role)).isEqualTo(wire)
            assertThat(WireValueMapper.toPersonRole(raw = wire)).isEqualTo(role)
        }
    }

    @Test
    fun `treatments map both ways`() {
        val cases =
            listOf(
                Treatment.None to "NONE",
                Treatment.FirstAid to "FIRST_AID",
                Treatment.MedicalTreatment to "MEDICAL_TREATMENT",
                Treatment.Hospital to "HOSPITAL",
                Treatment.Unknown(raw = "SURGERY") to "SURGERY",
            )

        cases.forEach { (treatment, wire) ->
            assertThat(WireValueMapper.toWire(treatment = treatment)).isEqualTo(wire)
            assertThat(WireValueMapper.toTreatment(raw = wire)).isEqualTo(treatment)
        }
    }

    @Test
    fun `immediate actions map both ways`() {
        val cases =
            listOf(
                ImmediateAction.None to "NONE",
                ImmediateAction.AreaCordonedOff to "AREA_CORDONED_OFF",
                ImmediateAction.EquipmentIsolated to "EQUIPMENT_ISOLATED",
                ImmediateAction.FirstAidGiven to "FIRST_AID_GIVEN",
                ImmediateAction.EmergencyServicesCalled to "EMERGENCY_SERVICES_CALLED",
                ImmediateAction.SupervisorNotified to "SUPERVISOR_NOTIFIED",
                ImmediateAction.AreaEvacuated to "AREA_EVACUATED",
                ImmediateAction.Other(description = "Put up signs") to "OTHER",
                ImmediateAction.Unknown(raw = "POWER_CUT") to "POWER_CUT",
            )

        cases.forEach { (action, wire) ->
            assertThat(WireValueMapper.toWire(action = action)).isEqualTo(wire)
            val otherDescription = (action as? ImmediateAction.Other)?.description
            assertThat(
                WireValueMapper.toImmediateAction(
                    raw = wire,
                    otherDescription = otherDescription,
                ),
            )
                .isEqualTo(action)
        }
    }

    @Test
    fun `user roles map both ways`() {
        val cases =
            listOf(
                UserRole.Technician to "TECHNICIAN",
                UserRole.Operator to "OPERATOR",
                UserRole.Unknown(raw = "ADMIN") to "ADMIN",
            )

        cases.forEach { (role, wire) ->
            assertThat(WireValueMapper.toWire(role = role)).isEqualTo(wire)
            assertThat(WireValueMapper.toUserRole(raw = wire)).isEqualTo(role)
        }
    }
}
