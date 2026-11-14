package com.jonesl7l.carinfg.core.domain.validation

import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentDetails
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentLocation
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSafety
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentType
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.model.person.InjuryDetails
import com.jonesl7l.carinfg.core.domain.model.person.Person
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
import kotlin.time.Instant

object IncidentValidator {
    private const val TITLE_MIN_LENGTH = 3
    private const val TITLE_MAX_LENGTH = 80
    private const val DESCRIPTION_MAX_LENGTH = 2000
    private const val MAX_PHOTOS = 5
    private val allowedClockSkew = 5.minutes

    fun validate(
        incident: Incident,
        now: Instant,
    ): List<FieldError> = buildList {
        validateDetails(details = incident.details, reportedAt = incident.reportedAt, now = now)
        validateLocation(location = incident.location)
        validateSafety(safety = incident.safety, persons = incident.persons)
        incident.persons.forEachIndexed { index, person -> validatePerson(index = index, person = person) }
        if (incident.photos.size > MAX_PHOTOS) {
            addError(
                field = IncidentField.Photos,
                reason = TooMany(max = MAX_PHOTOS),
            )
        }
    }

    private fun MutableList<FieldError>.validateDetails(
        details: IncidentDetails,
        reportedAt: Instant?,
        now: Instant,
    ) {
        val title = details.title.trim()
        // title is required and 3-80 chars once trimmed
        when {
            title.isEmpty() -> addError(field = IncidentField.Title, reason = Required)

            title.length < TITLE_MIN_LENGTH -> {
                addError(
                    field = IncidentField.Title,
                    reason = TooShort(min = TITLE_MIN_LENGTH),
                )
            }

            title.length > TITLE_MAX_LENGTH -> {
                addError(
                    field = IncidentField.Title,
                    reason = TooLong(max = TITLE_MAX_LENGTH),
                )
            }
        }

        val description = details.description.trim()
        // description is required and capped at 2000 chars
        when {
            description.isEmpty() -> addError(field = IncidentField.Description, reason = Required)

            description.length > DESCRIPTION_MAX_LENGTH -> {
                addError(
                    field = IncidentField.Description,
                    reason = TooLong(max = DESCRIPTION_MAX_LENGTH),
                )
            }
        }

        // occurredAt cant be in the future (bar small clock skew) or after it was reported
        when {
            details.occurredAt > now + allowedClockSkew -> {
                addError(field = IncidentField.OccurredAt, reason = InFuture)
            }

            reportedAt != null && details.occurredAt > reportedAt -> {
                addError(field = IncidentField.OccurredAt, reason = AfterReportedAt)
            }
        }

        // type is required, must be known, and other needs its text
        when (val type = details.type) {
            null -> addError(field = IncidentField.Type, reason = Required)

            is IncidentType.Unknown -> addError(field = IncidentField.Type, reason = UnknownValue)

            is IncidentType.Other -> if (type.description.isBlank()) {
                addError(field = IncidentField.Type, reason = OtherTextRequired)
            }

            else -> Unit
        }

        // severity is required and must be known
        when (details.severity) {
            null -> addError(field = IncidentField.Severity, reason = Required)
            is Severity.Unknown -> addError(field = IncidentField.Severity, reason = UnknownValue)
            else -> Unit
        }
    }

    private fun MutableList<FieldError>.validateLocation(location: IncidentLocation) {
        if (location.siteId == null) addError(field = IncidentField.Site, reason = Required)
        if (location.buildingId == null && location.unlistedLocation.isNullOrBlank()) {
            addError(field = IncidentField.Building, reason = Required)
        }
        if (location.equipmentOutOfService && location.assetId == null) {
            addError(field = IncidentField.EquipmentOutOfService, reason = AssetRequired)
        }
    }

    private fun MutableList<FieldError>.validateSafety(
        safety: IncidentSafety,
        persons: List<Person>,
    ) {
        when (safety.injuryOccurred) {
            null -> addError(field = IncidentField.InjuryOccurred, reason = Required)

            true -> if (persons.none { it.role == PersonRole.Injured }) {
                addError(field = IncidentField.InjuryOccurred, reason = InjuredPersonRequired)
            }

            false -> Unit
        }

        if (safety.hazardStillPresent == null) {
            addError(field = IncidentField.HazardStillPresent, reason = Required)
        }

        val actions = safety.immediateActions
        if (actions.isEmpty()) addError(field = IncidentField.ImmediateActions, reason = Required)
        if (ImmediateAction.None in actions && actions.size > 1) {
            addError(field = IncidentField.ImmediateActions, reason = NoneWithOthers)
        }
        if (actions.any { it is ImmediateAction.Other && it.description.isBlank() }) {
            addError(field = IncidentField.ImmediateActions, reason = OtherTextRequired)
        }
        if (actions.any { it is ImmediateAction.Unknown }) {
            addError(field = IncidentField.ImmediateActions, reason = UnknownValue)
        }
    }

    private fun MutableList<FieldError>.validatePerson(
        index: Int,
        person: Person,
    ) {
        if (person.name.isBlank()) addPersonError(index = index, field = PersonField.NAME, reason = Required)
        if (person.type is PersonType.Unknown) {
            addPersonError(index = index, field = PersonField.TYPE, reason = UnknownValue)
        }
        if (person.role is PersonRole.Unknown) {
            addPersonError(index = index, field = PersonField.ROLE, reason = UnknownValue)
        }
        if (person.role == PersonRole.Injured) validateInjury(index = index, injury = person.injury)
    }

    private fun MutableList<FieldError>.validateInjury(
        index: Int,
        injury: InjuryDetails?,
    ) {
        if (injury == null) {
            addPersonError(index = index, field = PersonField.INJURY_NATURE, reason = Required)
            addPersonError(index = index, field = PersonField.INJURY_BODY_PART, reason = Required)
            addPersonError(index = index, field = PersonField.INJURY_TREATMENT, reason = Required)
            return
        }
        if (injury.nature.isBlank()) {
            addPersonError(index = index, field = PersonField.INJURY_NATURE, reason = Required)
        }
        if (injury.bodyPart.isBlank()) {
            addPersonError(index = index, field = PersonField.INJURY_BODY_PART, reason = Required)
        }
        if (injury.treatment is Treatment.Unknown) {
            addPersonError(index = index, field = PersonField.INJURY_TREATMENT, reason = UnknownValue)
        }
    }

    private fun MutableList<FieldError>.addError(
        field: IncidentField,
        reason: ValidationReason,
    ) {
        add(FieldError(field = field, reason = reason))
    }

    private fun MutableList<FieldError>.addPersonError(
        index: Int,
        field: PersonField,
        reason: ValidationReason,
    ) {
        addError(
            field = IncidentField.Person(index = index, field = field),
            reason = reason,
        )
    }
}
