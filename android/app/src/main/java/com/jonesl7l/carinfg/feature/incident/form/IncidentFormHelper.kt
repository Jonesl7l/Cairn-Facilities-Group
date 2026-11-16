package com.jonesl7l.carinfg.feature.incident.form

import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentDetails
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentLocation
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSafety
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import com.jonesl7l.carinfg.core.domain.model.incident.OperatorFields
import com.jonesl7l.carinfg.core.domain.model.incident.ServerFields
import com.jonesl7l.carinfg.core.domain.model.person.InjuryDetails
import com.jonesl7l.carinfg.core.domain.model.person.Person
import com.jonesl7l.carinfg.core.domain.model.person.PersonRole
import com.jonesl7l.carinfg.core.domain.model.person.PersonType
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.sync.SyncStatus
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

object IncidentFormHelper {
    // region rules
    fun applyRules(
        previous: IncidentFormFields,
        next: IncidentFormFields,
    ): IncidentFormFields = next.copy(
        location = locationRules(
            previous = previous.location,
            next = next.location,
        ),
        safety = next.safety.copy(
            immediateActions = actionRules(
                previous = previous.safety.immediateActions,
                next = next.safety.immediateActions,
            ),
        ),
        persons = next.persons.map(::personRules),
    )

    private fun locationRules(
        previous: IncidentLocation,
        next: IncidentLocation,
    ): IncidentLocation {
        var location = next
        if (location.siteId != previous.siteId) {
            location = location.copy(
                buildingId = null,
                unlistedLocation = null,
            )
        }
        if (location.buildingId != null) location = location.copy(unlistedLocation = null)
        if (location.buildingId != previous.buildingId) location = location.copy(floorId = null)
        if (location.floorId != previous.floorId) location = location.copy(roomId = null)
        if (location.roomId != previous.roomId) location = location.copy(assetId = null)
        if (location.assetId == null) location = location.copy(equipmentOutOfService = false)
        return location
    }

    private fun actionRules(
        previous: Set<ImmediateAction>,
        next: Set<ImmediateAction>,
    ): Set<ImmediateAction> {
        val added = next - previous
        return when {
            ImmediateAction.None in added -> setOf(ImmediateAction.None)
            added.isNotEmpty() -> next - ImmediateAction.None
            else -> next
        }
    }

    private fun personRules(person: Person): Person = person.copy(
        department = person.department.takeIf { person.type == PersonType.Employee },
        position = person.position.takeIf { person.type == PersonType.Employee },
        company = person.company.takeIf { person.type == PersonType.Contractor },
        injury = if (person.role == PersonRole.Injured) person.injury ?: InjuryDetails() else null,
    )
    // endregion

    fun optionsFor(
        references: ReferenceOptions,
        location: IncidentLocation,
    ) = ReferenceOptions(
        sites = references.sites,
        buildings = references.buildings.filter { it.siteId == location.siteId },
        floors = references.floors.filter { it.buildingId == location.buildingId },
        rooms = references.rooms.filter { it.floorId == location.floorId },
        assets = references.assets.filter { it.roomId == location.roomId },
    )

    fun canAddPerson(persons: List<Person>): Boolean = persons.lastOrNull()?.hasData() ?: true

    private fun Person.hasData(): Boolean = name.isNotBlank() ||
        type != null ||
        role != null ||
        listOf(
            department,
            position,
            company,
            contactNumber,
        ).any { !it.isNullOrBlank() }

    fun newIncident(
        id: String,
        reporterId: String,
        now: Instant,
        timeZone: TimeZone,
    ) = Incident(
        id = id,
        referenceNumber = null,
        reporterId = reporterId,
        reportedAt = null,
        gpsLocation = null,
        details = IncidentDetails(
            title = "",
            description = "",
            occurredAt = now,
            occurredTimeZone = timeZone,
            type = null,
            severity = null,
        ),
        location = IncidentLocation(),
        safety = IncidentSafety(),
        persons = emptyList(),
        photos = emptyList(),
        additionalNotes = null,
        operatorFields = OperatorFields(
            status = IncidentStatus.Open,
            assignedTo = null,
            dueDate = null,
            rootCause = null,
            correctiveActions = null,
            regulatoryReportable = null,
            supervisorAcknowledged = null,
        ),
        serverFields = ServerFields(
            createdAt = null,
            updatedAt = null,
            deletedAt = null,
            deletedBy = null,
        ),
        syncStatus = SyncStatus(
            state = SyncState.DRAFT,
            conflictReason = null,
            attempts = 0,
            lastError = null,
        ),
    )
}
