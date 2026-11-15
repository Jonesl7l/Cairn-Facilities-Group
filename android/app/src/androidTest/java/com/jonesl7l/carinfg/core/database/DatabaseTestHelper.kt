package com.jonesl7l.carinfg.core.database

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.jonesl7l.carinfg.core.database.entity.incident.GpsLocationColumns
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.InjuryColumn
import com.jonesl7l.carinfg.core.database.entity.incident.PersonColumn
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity
import com.jonesl7l.carinfg.core.database.entity.reference.AssetEntity
import com.jonesl7l.carinfg.core.database.entity.reference.BuildingEntity
import com.jonesl7l.carinfg.core.database.entity.reference.FloorEntity
import com.jonesl7l.carinfg.core.database.entity.reference.ReferenceEntities
import com.jonesl7l.carinfg.core.database.entity.reference.RoomEntity
import com.jonesl7l.carinfg.core.database.entity.reference.SiteEntity
import com.jonesl7l.carinfg.core.database.entity.reference.TechnicianEntity
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import com.jonesl7l.carinfg.core.domain.model.photo.PhotoUploadState
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

object DatabaseTestHelper {
    const val INCIDENT_ID = "incident-1"
    val now: Instant = Instant.parse(input = "2026-11-13T12:00:00Z")

    fun inMemoryDatabase(): AppDatabase = Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .build()

    fun referenceEntities(
        sites: List<SiteEntity> = listOf(siteEntity()),
        buildings: List<BuildingEntity> = listOf(buildingEntity()),
        floors: List<FloorEntity> = listOf(floorEntity()),
        rooms: List<RoomEntity> = listOf(roomEntity()),
        assets: List<AssetEntity> = listOf(assetEntity()),
        technicians: List<TechnicianEntity> = listOf(technicianEntity()),
    ) = ReferenceEntities(
        sites = sites,
        buildings = buildings,
        floors = floors,
        rooms = rooms,
        assets = assets,
        technicians = technicians,
    )

    fun siteEntity(
        id: String = "site-1",
        name: String = "Head Office",
    ) = SiteEntity(
        id = id,
        name = name,
    )

    fun buildingEntity(
        id: String = "building-1",
        siteId: String = "site-1",
        name: String = "North Block",
    ) = BuildingEntity(
        id = id,
        siteId = siteId,
        name = name,
    )

    fun floorEntity(
        id: String = "floor-1",
        buildingId: String = "building-1",
        name: String = "Ground Floor",
    ) = FloorEntity(
        id = id,
        buildingId = buildingId,
        name = name,
    )

    fun roomEntity(
        id: String = "room-1",
        floorId: String = "floor-1",
        name: String = "Lobby",
    ) = RoomEntity(
        id = id,
        floorId = floorId,
        name = name,
    )

    fun assetEntity(
        id: String = "asset-1",
        roomId: String = "room-1",
        name: String = "Revolving Door",
        code: String = "DOOR-001",
    ) = AssetEntity(
        id = id,
        roomId = roomId,
        name = name,
        code = code,
    )

    fun technicianEntity(
        id: String = "u1",
        username: String = "tech1",
        displayName: String = "Tech One",
    ) = TechnicianEntity(
        id = id,
        username = username,
        displayName = displayName,
    )

    fun incidentEntity(
        id: String = INCIDENT_ID,
        referenceNumber: String? = "INC-2026-00001",
        reporterId: String = "u1",
        reportedAt: Instant? = now,
        gpsLocation: GpsLocationColumns? = gpsLocationColumns(),
        title: String = "Wet floor in lobby",
        description: String = "Water pooling by the main entrance.",
        occurredAt: Instant = now - 1.hours,
        occurredTimeZone: String = "Europe/London",
        incidentType: String? = "SLIP_TRIP_FALL",
        incidentTypeOther: String? = null,
        severity: String? = "MEDIUM",
        severityRank: Int = 2,
        siteId: String? = "site-1",
        buildingId: String? = "building-1",
        floorId: String? = "floor-1",
        roomId: String? = "room-1",
        unlistedLocation: String? = null,
        exactSpot: String? = "By the revolving door",
        assetId: String? = "asset-1",
        equipmentOutOfService: Boolean = true,
        injuryOccurred: Boolean? = true,
        hazardStillPresent: Boolean? = false,
        immediateActions: List<String> = listOf("FIRST_AID_GIVEN", "OTHER"),
        immediateActionsOther: String? = "Mopped the floor",
        persons: List<PersonColumn> = listOf(personColumn()),
        additionalNotes: String? = "Cleaner informed",
        status: String = "OPEN",
        assignedTo: String? = "operator1",
        dueDate: LocalDate? = LocalDate(year = 2026, month = 11, day = 20),
        rootCause: String? = null,
        correctiveActions: String? = null,
        regulatoryReportable: Boolean? = false,
        supervisorAcknowledged: Boolean? = true,
        createdAt: Instant? = now,
        updatedAt: Instant? = now,
        serverVersion: Int? = 1,
        changeSeq: Long? = 1,
        deletedAt: Instant? = null,
        deletedBy: String? = null,
        syncState: SyncState = SyncState.SYNCED,
        conflictReason: ConflictReason? = null,
        previousSyncState: SyncState? = null,
        undoDeadline: Instant? = null,
        localRevision: Long = 0,
        lastSyncError: String? = null,
        syncAttempts: Int = 0,
        localUpdatedAt: Instant = now,
    ) = IncidentEntity(
        id = id,
        referenceNumber = referenceNumber,
        reporterId = reporterId,
        reportedAt = reportedAt,
        gpsLocation = gpsLocation,
        title = title,
        description = description,
        occurredAt = occurredAt,
        occurredTimeZone = occurredTimeZone,
        incidentType = incidentType,
        incidentTypeOther = incidentTypeOther,
        severity = severity,
        severityRank = severityRank,
        siteId = siteId,
        buildingId = buildingId,
        floorId = floorId,
        roomId = roomId,
        unlistedLocation = unlistedLocation,
        exactSpot = exactSpot,
        assetId = assetId,
        equipmentOutOfService = equipmentOutOfService,
        injuryOccurred = injuryOccurred,
        hazardStillPresent = hazardStillPresent,
        immediateActions = immediateActions,
        immediateActionsOther = immediateActionsOther,
        persons = persons,
        additionalNotes = additionalNotes,
        status = status,
        assignedTo = assignedTo,
        dueDate = dueDate,
        rootCause = rootCause,
        correctiveActions = correctiveActions,
        regulatoryReportable = regulatoryReportable,
        supervisorAcknowledged = supervisorAcknowledged,
        createdAt = createdAt,
        updatedAt = updatedAt,
        serverVersion = serverVersion,
        changeSeq = changeSeq,
        deletedAt = deletedAt,
        deletedBy = deletedBy,
        syncState = syncState,
        conflictReason = conflictReason,
        previousSyncState = previousSyncState,
        undoDeadline = undoDeadline,
        localRevision = localRevision,
        lastSyncError = lastSyncError,
        syncAttempts = syncAttempts,
        localUpdatedAt = localUpdatedAt,
    )

    fun draftIncidentEntity(id: String = INCIDENT_ID) = incidentEntity(
        id = id,
        referenceNumber = null,
        reportedAt = null,
        gpsLocation = null,
        incidentType = null,
        severity = null,
        severityRank = 0,
        siteId = null,
        buildingId = null,
        floorId = null,
        roomId = null,
        exactSpot = null,
        assetId = null,
        equipmentOutOfService = false,
        injuryOccurred = null,
        hazardStillPresent = null,
        immediateActions = emptyList(),
        immediateActionsOther = null,
        persons = emptyList(),
        additionalNotes = null,
        assignedTo = null,
        dueDate = null,
        regulatoryReportable = null,
        supervisorAcknowledged = null,
        createdAt = null,
        updatedAt = null,
        serverVersion = null,
        changeSeq = null,
        syncState = SyncState.DRAFT,
    )

    fun gpsLocationColumns(
        latitude: Double = 51.5074,
        longitude: Double = -0.1278,
        accuracyMeters: Float = 12f,
    ) = GpsLocationColumns(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
    )

    fun personColumn(
        id: String = "person-1",
        name: String = "Alex Smith",
        personType: String = "EMPLOYEE",
        role: String = "INJURED",
        department: String? = "Facilities",
        position: String? = "Cleaner",
        company: String? = null,
        contactNumber: String? = "07700900000",
        injury: InjuryColumn? = injuryColumn(),
    ) = PersonColumn(
        id = id,
        name = name,
        personType = personType,
        role = role,
        department = department,
        position = position,
        company = company,
        contactNumber = contactNumber,
        injury = injury,
    )

    fun injuryColumn(
        nature: String = "Sprain",
        bodyPart: String = "Left ankle",
        treatment: String = "FIRST_AID",
        notes: String? = null,
    ) = InjuryColumn(
        nature = nature,
        bodyPart = bodyPart,
        treatment = treatment,
        notes = notes,
    )

    fun photoEntity(
        position: Int = 0,
        id: String = "photo-$position",
        incidentId: String = INCIDENT_ID,
        localPath: String? = null,
        remoteUrl: String? = "uploads/incidents/$incidentId/$id.jpg",
        uploadState: PhotoUploadState = PhotoUploadState.UPLOADED,
    ) = PhotoEntity(
        id = id,
        incidentId = incidentId,
        position = position,
        localPath = localPath,
        remoteUrl = remoteUrl,
        uploadState = uploadState,
    )

    fun syncMetaEntity(
        lastChangeSeq: Long = 42,
        databaseId: String? = "db-1",
        announcedChangeSeq: Long = 42,
    ) = SyncMetaEntity(
        lastChangeSeq = lastChangeSeq,
        databaseId = databaseId,
        announcedChangeSeq = announcedChangeSeq,
    )
}
