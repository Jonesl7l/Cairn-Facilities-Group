package com.jonesl7l.carinfg.core.domain.validation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incident
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incidentDetails
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incidentLocation
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incidentSafety
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.injuryDetails
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.now
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.person
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.photo
import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentType
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.model.person.PersonRole
import com.jonesl7l.carinfg.core.domain.model.person.PersonType
import com.jonesl7l.carinfg.core.domain.model.person.Treatment
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason.AfterReportedAt
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason.AssetRequired
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason.InFuture
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason.InjuredPersonRequired
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason.NoneWithOthers
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason.OtherTextRequired
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason.Required
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason.TooLong
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason.TooMany
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason.TooShort
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason.UnknownValue
import kotlin.time.Duration.Companion.minutes
import org.junit.Test

class IncidentValidatorTest {
    @Test
    fun `valid incident has no errors`() {
        assertThat(incident().errors()).isEmpty()
    }

    @Test
    fun `title is required, trimmed and between 3 and 80 characters`() {
        assertThat(incident(details = incidentDetails(title = "   ")).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Title,
                    reason = Required,
                ),
            )
        assertThat(incident(details = incidentDetails(title = " ab ")).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Title,
                    reason = TooShort(min = 3),
                ),
            )
        assertThat(incident(details = incidentDetails(title = "a".repeat(81))).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Title,
                    reason = TooLong(max = 80),
                ),
            )
        assertThat(incident(details = incidentDetails(title = "a".repeat(80))).errors()).isEmpty()
    }

    @Test
    fun `description is required and at most 2000 characters`() {
        assertThat(incident(details = incidentDetails(description = "")).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Description,
                    reason = Required,
                ),
            )
        assertThat(incident(details = incidentDetails(description = "a".repeat(2001))).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Description,
                    reason = TooLong(max = 2000),
                ),
            )
    }

    @Test
    fun `occurred at allows small clock skew but not the future`() {
        val withinSkew = incident(
            reportedAt = null,
            details = incidentDetails(occurredAt = now + 5.minutes),
        )
        val beyondSkew = incident(
            reportedAt = null,
            details = incidentDetails(occurredAt = now + 6.minutes),
        )

        assertThat(withinSkew.errors()).isEmpty()
        assertThat(beyondSkew.errors()).containsExactly(
            FieldError(
                field = IncidentField.OccurredAt,
                reason = InFuture,
            ),
        )
    }

    @Test
    fun `occurred at cannot be after reported at`() {
        assertThat(incident(details = incidentDetails(occurredAt = now + 1.minutes)).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.OccurredAt,
                    reason = AfterReportedAt,
                ),
            )
    }

    @Test
    fun `type is required, known, and other needs text`() {
        assertThat(incident(details = incidentDetails(type = null)).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Type,
                    reason = Required,
                ),
            )
        assertThat(incident(details = incidentDetails(type = IncidentType.Unknown(raw = "FLOOD"))).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Type,
                    reason = UnknownValue,
                ),
            )
        assertThat(incident(details = incidentDetails(type = IncidentType.Other(description = " "))).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Type,
                    reason = OtherTextRequired,
                ),
            )
        assertThat(incident(details = incidentDetails(type = IncidentType.Other(description = "Pest"))).errors())
            .isEmpty()
    }

    @Test
    fun `severity is required and known`() {
        assertThat(incident(details = incidentDetails(severity = null)).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Severity,
                    reason = Required,
                ),
            )
        assertThat(incident(details = incidentDetails(severity = Severity.Unknown(raw = "SEVERE"))).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Severity,
                    reason = UnknownValue,
                ),
            )
    }

    @Test
    fun `site is required and building can be replaced by an unlisted location`() {
        assertThat(incident(location = incidentLocation(siteId = null)).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Site,
                    reason = Required,
                ),
            )
        assertThat(incident(location = incidentLocation(buildingId = null)).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Building,
                    reason = Required,
                ),
            )
        assertThat(
            incident(
                location = incidentLocation(
                    buildingId = null,
                    unlistedLocation = "Car park",
                ),
            ).errors(),
        )
            .isEmpty()
    }

    @Test
    fun `equipment out of service needs an asset`() {
        assertThat(incident(location = incidentLocation(equipmentOutOfService = true)).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.EquipmentOutOfService,
                    reason = AssetRequired,
                ),
            )
        assertThat(
            incident(
                location = incidentLocation(
                    equipmentOutOfService = true,
                    assetId = "a1",
                ),
            ).errors(),
        )
            .isEmpty()
    }

    @Test
    fun `injury and hazard answers are required`() {
        assertThat(
            incident(
                safety = incidentSafety(
                    injuryOccurred = null,
                    hazardStillPresent = null,
                ),
            ).errors(),
        )
            .containsExactlyInAnyOrder(
                FieldError(
                    field = IncidentField.InjuryOccurred,
                    reason = Required,
                ),
                FieldError(
                    field = IncidentField.HazardStillPresent,
                    reason = Required,
                ),
            )
    }

    @Test
    fun `an injury needs an injured person`() {
        val safety = incidentSafety(injuryOccurred = true)

        val witnessOnly = incident(
            safety = safety,
            persons = listOf(person(role = PersonRole.Witness)),
        )
        val withInjured = incident(
            safety = safety,
            persons = listOf(person(role = PersonRole.Injured)),
        )

        assertThat(witnessOnly.errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.InjuryOccurred,
                    reason = InjuredPersonRequired,
                ),
            )
        assertThat(withInjured.errors()).isEmpty()
    }

    @Test
    fun `immediate actions need at least one, none alone, and other with text`() {
        assertThat(actionErrors())
            .containsExactly(
                FieldError(
                    field = IncidentField.ImmediateActions,
                    reason = Required,
                ),
            )
        assertThat(actionErrors(ImmediateAction.None, ImmediateAction.AreaEvacuated))
            .containsExactly(
                FieldError(
                    field = IncidentField.ImmediateActions,
                    reason = NoneWithOthers,
                ),
            )
        assertThat(actionErrors(ImmediateAction.Other(description = "")))
            .containsExactly(
                FieldError(
                    field = IncidentField.ImmediateActions,
                    reason = OtherTextRequired,
                ),
            )
        assertThat(actionErrors(ImmediateAction.Unknown(raw = "DRAINED")))
            .containsExactly(
                FieldError(
                    field = IncidentField.ImmediateActions,
                    reason = UnknownValue,
                ),
            )
        assertThat(actionErrors(ImmediateAction.None)).isEmpty()
    }

    @Test
    fun `persons need a name and known type and role`() {
        val invalid = person(
            name = " ",
            type = PersonType.Unknown(raw = "INTERN"),
            role = PersonRole.Unknown(raw = "HOST"),
        )

        assertThat(incident(persons = listOf(person(), invalid)).errors()).containsExactlyInAnyOrder(
            personError(
                index = 1,
                field = PersonField.NAME,
                reason = Required,
            ),
            personError(
                index = 1,
                field = PersonField.TYPE,
                reason = UnknownValue,
            ),
            personError(
                index = 1,
                field = PersonField.ROLE,
                reason = UnknownValue,
            ),
        )
    }

    @Test
    fun `persons need a type, role and treatment chosen`() {
        val unchosen = person(
            type = null,
            role = PersonRole.Injured,
            injury = injuryDetails(treatment = null),
        )
        val noRole = person(role = null)

        assertThat(incident(persons = listOf(unchosen, noRole)).errors()).containsExactlyInAnyOrder(
            personError(
                index = 0,
                field = PersonField.TYPE,
                reason = Required,
            ),
            personError(
                index = 0,
                field = PersonField.INJURY_TREATMENT,
                reason = Required,
            ),
            personError(
                index = 1,
                field = PersonField.ROLE,
                reason = Required,
            ),
        )
    }

    @Test
    fun `injured persons need injury details`() {
        val missing = person(
            role = PersonRole.Injured,
            injury = null,
        )
        val blank = person(
            role = PersonRole.Injured,
            injury = injuryDetails(
                nature = "",
                bodyPart = "",
                treatment = Treatment.Unknown(raw = "SURGERY"),
            ),
        )

        assertThat(incident(persons = listOf(missing)).errors()).containsExactlyInAnyOrder(
            personError(
                index = 0,
                field = PersonField.INJURY_NATURE,
                reason = Required,
            ),
            personError(
                index = 0,
                field = PersonField.INJURY_BODY_PART,
                reason = Required,
            ),
            personError(
                index = 0,
                field = PersonField.INJURY_TREATMENT,
                reason = Required,
            ),
        )
        assertThat(incident(persons = listOf(blank)).errors()).containsExactlyInAnyOrder(
            personError(
                index = 0,
                field = PersonField.INJURY_NATURE,
                reason = Required,
            ),
            personError(
                index = 0,
                field = PersonField.INJURY_BODY_PART,
                reason = Required,
            ),
            personError(
                index = 0,
                field = PersonField.INJURY_TREATMENT,
                reason = UnknownValue,
            ),
        )
    }

    @Test
    fun `at most five photos`() {
        assertThat(incident(photos = List(5) { photo(position = it) }).errors()).isEmpty()
        assertThat(incident(photos = List(6) { photo(position = it) }).errors())
            .containsExactly(
                FieldError(
                    field = IncidentField.Photos,
                    reason = TooMany(max = 5),
                ),
            )
    }

    private fun Incident.errors(): List<FieldError> = IncidentValidator.validate(
        incident = this,
        now = now,
    )

    private fun actionErrors(vararg actions: ImmediateAction): List<FieldError> =
        incident(safety = incidentSafety(immediateActions = actions.toSet())).errors()

    private fun personError(
        index: Int,
        field: PersonField,
        reason: ValidationReason,
    ) = FieldError(
        field = IncidentField.Person(
            index = index,
            field = field,
        ),
        reason = reason,
    )
}
