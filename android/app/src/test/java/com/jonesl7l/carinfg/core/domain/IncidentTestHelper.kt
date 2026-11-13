package com.jonesl7l.carinfg.core.domain

import com.jonesl7l.carinfg.core.domain.model.incident.GpsLocation
import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentDetails
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentLocation
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSafety
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentType
import com.jonesl7l.carinfg.core.domain.model.incident.OperatorFields
import com.jonesl7l.carinfg.core.domain.model.incident.ServerFields
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.model.person.InjuryDetails
import com.jonesl7l.carinfg.core.domain.model.person.Person
import com.jonesl7l.carinfg.core.domain.model.person.PersonRole
import com.jonesl7l.carinfg.core.domain.model.person.PersonType
import com.jonesl7l.carinfg.core.domain.model.person.Treatment
import com.jonesl7l.carinfg.core.domain.model.photo.Photo
import com.jonesl7l.carinfg.core.domain.model.photo.PhotoUploadState
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.sync.SyncStatus
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

object IncidentTestHelper {
    val NOW: Instant = Instant.parse("2026-11-13T12:00:00Z")
    const val INCIDENT_ID = "incident-1"
    const val REPORTER_ID = "tech1"

    fun incident(
        id: String = INCIDENT_ID,
        referenceNumber: String? = "INC-2026-00001",
        reporterId: String = REPORTER_ID,
        reportedAt: Instant? = NOW,
        gpsLocation: GpsLocation? = null,
        details: IncidentDetails = incidentDetails(),
        location: IncidentLocation = incidentLocation(),
        safety: IncidentSafety = incidentSafety(),
        persons: List<Person> = emptyList(),
        photos: List<Photo> = emptyList(),
        additionalNotes: String? = null,
        operatorFields: OperatorFields = operatorFields(),
        serverFields: ServerFields = serverFields(),
        syncStatus: SyncStatus = syncStatus(),
    ) = Incident(
        id = id,
        referenceNumber = referenceNumber,
        reporterId = reporterId,
        reportedAt = reportedAt,
        gpsLocation = gpsLocation,
        details = details,
        location = location,
        safety = safety,
        persons = persons,
        photos = photos,
        additionalNotes = additionalNotes,
        operatorFields = operatorFields,
        serverFields = serverFields,
        syncStatus = syncStatus,
    )

    fun incidentDetails(
        title: String = "Leak in plant room",
        description: String = "Water pooling under the main boiler.",
        occurredAt: Instant = NOW - 1.hours,
        occurredTimeZone: TimeZone = TimeZone.UTC,
        type: IncidentType? = IncidentType.WaterLeak,
        severity: Severity? = Severity.Medium,
    ) = IncidentDetails(
        title = title,
        description = description,
        occurredAt = occurredAt,
        occurredTimeZone = occurredTimeZone,
        type = type,
        severity = severity,
    )

    fun incidentLocation(
        siteId: String? = "s1",
        buildingId: String? = "b1",
        floorId: String? = null,
        roomId: String? = null,
        unlistedLocation: String? = null,
        exactSpot: String? = null,
        assetId: String? = null,
        equipmentOutOfService: Boolean = false,
    ) = IncidentLocation(
        siteId = siteId,
        buildingId = buildingId,
        floorId = floorId,
        roomId = roomId,
        unlistedLocation = unlistedLocation,
        exactSpot = exactSpot,
        assetId = assetId,
        equipmentOutOfService = equipmentOutOfService,
    )

    fun incidentSafety(
        injuryOccurred: Boolean? = false,
        hazardStillPresent: Boolean? = true,
        immediateActions: Set<ImmediateAction> = setOf(ImmediateAction.AreaCordonedOff),
    ) = IncidentSafety(
        injuryOccurred = injuryOccurred,
        hazardStillPresent = hazardStillPresent,
        immediateActions = immediateActions,
    )

    fun operatorFields(
        status: IncidentStatus = IncidentStatus.Open,
        assignedTo: String? = null,
        dueDate: LocalDate? = null,
        rootCause: String? = null,
        correctiveActions: String? = null,
        regulatoryReportable: Boolean? = null,
        supervisorAcknowledged: Boolean? = null,
    ) = OperatorFields(
        status = status,
        assignedTo = assignedTo,
        dueDate = dueDate,
        rootCause = rootCause,
        correctiveActions = correctiveActions,
        regulatoryReportable = regulatoryReportable,
        supervisorAcknowledged = supervisorAcknowledged,
    )

    fun serverFields(
        createdAt: Instant? = NOW,
        updatedAt: Instant? = NOW,
        deletedAt: Instant? = null,
        deletedBy: String? = null,
    ) = ServerFields(
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        deletedBy = deletedBy,
    )

    fun syncStatus(
        state: SyncState = SyncState.SYNCED,
        conflictReason: ConflictReason? = null,
        attempts: Int = 0,
        lastError: String? = null,
    ) = SyncStatus(
        state = state,
        conflictReason = conflictReason,
        attempts = attempts,
        lastError = lastError,
    )

    fun person(
        id: String = "person-1",
        name: String = "Sam Taylor",
        type: PersonType = PersonType.Employee,
        role: PersonRole = PersonRole.Witness,
        department: String? = null,
        position: String? = null,
        company: String? = null,
        contactNumber: String? = null,
        injury: InjuryDetails? = if (role == PersonRole.Injured) injuryDetails() else null,
    ) = Person(
        id = id,
        name = name,
        type = type,
        role = role,
        department = department,
        position = position,
        company = company,
        contactNumber = contactNumber,
        injury = injury,
    )

    fun injuryDetails(
        nature: String = "Cut",
        bodyPart: String = "Hand",
        treatment: Treatment = Treatment.FirstAid,
        notes: String? = null,
    ) = InjuryDetails(
        nature = nature,
        bodyPart = bodyPart,
        treatment = treatment,
        notes = notes,
    )

    fun photo(
        position: Int = 0,
        id: String = "photo-$position",
        incidentId: String = INCIDENT_ID,
        caption: String? = null,
        localPath: String? = null,
        remoteUrl: String? = "uploads/incidents/$incidentId/$id.jpg",
        width: Int = 1600,
        height: Int = 1200,
        sizeBytes: Long = 250_000,
        takenAt: Instant = NOW,
        uploadState: PhotoUploadState = PhotoUploadState.UPLOADED,
    ) = Photo(
        id = id,
        incidentId = incidentId,
        position = position,
        caption = caption,
        localPath = localPath,
        remoteUrl = remoteUrl,
        width = width,
        height = height,
        sizeBytes = sizeBytes,
        takenAt = takenAt,
        uploadState = uploadState,
    )
}
