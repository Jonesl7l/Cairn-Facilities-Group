package com.jonesl7l.carinfg.core.data.mapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.jonesl7l.carinfg.core.database.EntityTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.injuryColumn
import com.jonesl7l.carinfg.core.database.EntityTestHelper.now
import com.jonesl7l.carinfg.core.database.EntityTestHelper.personColumn
import com.jonesl7l.carinfg.core.database.EntityTestHelper.photoEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.reference.ReferenceEntities
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incident
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incidentDetails
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incidentLocation
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incidentSafety
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.injuryDetails
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.operatorFields
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.person
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.photo
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.serverFields
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.syncStatus
import com.jonesl7l.carinfg.core.domain.model.incident.GpsLocation
import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentType
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.model.person.PersonRole
import com.jonesl7l.carinfg.core.domain.model.person.PersonType
import com.jonesl7l.carinfg.core.domain.model.person.Treatment
import com.jonesl7l.carinfg.core.domain.model.photo.PhotoUploadState
import com.jonesl7l.carinfg.core.domain.model.reference.Asset
import com.jonesl7l.carinfg.core.domain.model.reference.Building
import com.jonesl7l.carinfg.core.domain.model.reference.Floor
import com.jonesl7l.carinfg.core.domain.model.reference.Room
import com.jonesl7l.carinfg.core.domain.model.reference.Site
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.network.ApiTestHelper.REPORTER_ID
import com.jonesl7l.carinfg.core.network.ApiTestHelper.incidentWriteRequest
import com.jonesl7l.carinfg.core.network.ApiTestHelper.referenceDataDto
import com.jonesl7l.carinfg.core.network.dto.incident.DeleteIncidentRequest
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Test

class EntityExtensionsTest {
    // region IncidentWithPhotos
    @Test
    fun `toDomain maps every field and sorts photos by position`() {
        val photos = listOf(photoEntity(position = 1), photoEntity(position = 0))
        val entity =
            incidentEntity(
                syncState = SyncState.NEEDS_ATTENTION,
                conflictReason = ConflictReason.STATUS_LOCKED,
                syncAttempts = 2,
                lastSyncError = "STATUS_LOCKED",
            )

        val expected =
            incident(
                gpsLocation =
                    GpsLocation(
                        latitude = 51.5074,
                        longitude = -0.1278,
                        accuracyMeters = 12f,
                    ),
                reporterId = "u1",
                details =
                    incidentDetails(
                        title = "Wet floor in lobby",
                        description = "Water pooling by the main entrance.",
                        occurredAt = now - 1.hours,
                        occurredTimeZone = TimeZone.of(zoneId = "Europe/London"),
                        type = IncidentType.SlipTripFall,
                        severity = Severity.Medium,
                    ),
                location =
                    incidentLocation(
                        siteId = "site-1",
                        buildingId = "building-1",
                        floorId = "floor-1",
                        roomId = "room-1",
                        exactSpot = "By the revolving door",
                        assetId = "asset-1",
                        equipmentOutOfService = true,
                    ),
                safety =
                    incidentSafety(
                        injuryOccurred = true,
                        hazardStillPresent = false,
                        immediateActions =
                            setOf(
                                ImmediateAction.FirstAidGiven,
                                ImmediateAction.Other(description = "Mopped the floor"),
                            ),
                    ),
                persons =
                    listOf(
                        person(
                            name = "Alex Smith",
                            type = PersonType.Employee,
                            role = PersonRole.Injured,
                            department = "Facilities",
                            position = "Cleaner",
                            contactNumber = "07700900000",
                            injury =
                                injuryDetails(
                                    nature = "Sprain",
                                    bodyPart = "Left ankle",
                                    treatment = Treatment.FirstAid,
                                ),
                        ),
                    ),
                photos = listOf(photo(position = 0), photo(position = 1)),
                additionalNotes = "Cleaner informed",
                operatorFields =
                    operatorFields(
                        status = IncidentStatus.Open,
                        assignedTo = "operator1",
                        dueDate = LocalDate(
                            year = 2026,
                            month = 11,
                            day = 20,
                        ),
                        regulatoryReportable = false,
                        supervisorAcknowledged = true,
                    ),
                serverFields = serverFields(),
                syncStatus =
                    syncStatus(
                        state = SyncState.NEEDS_ATTENTION,
                        conflictReason = ConflictReason.STATUS_LOCKED,
                        attempts = 2,
                        lastError = "STATUS_LOCKED",
                    ),
            )
        assertThat(
            IncidentWithPhotos(
                incident = entity,
                photos = photos,
            ).toDomain(),
        ).isEqualTo(expected)
    }

    @Test
    fun `an unknown time zone falls back to utc`() {
        val entity = incidentEntity(occurredTimeZone = "Mars/Olympus_Mons")

        val domain = toDomain(entity = entity)

        assertThat(domain.details.occurredTimeZone).isEqualTo(TimeZone.UTC)
    }

    @Test
    fun `a complete incident maps to a write request`() {
        val withPhotos =
            IncidentWithPhotos(
                incident = incidentEntity(reporterId = REPORTER_ID),
                photos = listOf(photoEntity(position = 0)),
            )

        assertThat(withPhotos.toWriteRequest()).isEqualTo(incidentWriteRequest())
    }

    @Test
    fun `an incident missing a required field gives no write request`() {
        val incomplete =
            listOf(
                incidentEntity(reportedAt = null),
                incidentEntity(incidentType = null),
                incidentEntity(severity = null),
                incidentEntity(siteId = null),
                incidentEntity(injuryOccurred = null),
                incidentEntity(hazardStillPresent = null),
                incidentEntity(persons = listOf(personColumn(personType = null))),
                incidentEntity(persons = listOf(personColumn(role = null))),
                incidentEntity(persons = listOf(personColumn(injury = injuryColumn(treatment = null)))),
            )

        incomplete.forEach { entity ->
            assertThat(
                IncidentWithPhotos(
                    incident = entity,
                    photos = emptyList(),
                ).toWriteRequest(),
            ).isNull()
        }
    }

    @Test
    fun `only uploaded photos are sent, in position order`() {
        val photos =
            listOf(
                photoEntity(position = 1),
                photoEntity(position = 0),
                photoEntity(
                    position = 2,
                    remoteUrl = null,
                    uploadState = PhotoUploadState.PENDING,
                ),
            )

        val request = IncidentWithPhotos(
            incident = incidentEntity(),
            photos = photos,
        ).toWriteRequest()

        assertThat(request?.photos?.map { it.id }).isEqualTo(listOf("photo-0", "photo-1"))
    }
    // endregion

    // region IncidentEntity
    @Test
    fun `a pending delete maps to a delete request`() {
        val entity =
            incidentEntity(
                deletedAt = now,
                deletedBy = REPORTER_ID,
            )

        val expected =
            DeleteIncidentRequest(
                deletedAt = now,
                deletedBy = REPORTER_ID,
            )
        assertThat(entity.toDeleteRequest()).isEqualTo(expected)
    }

    @Test
    fun `a delete without its time or user gives no delete request`() {
        assertThat(
            incidentEntity(
                deletedAt = null,
                deletedBy = REPORTER_ID,
            ).toDeleteRequest(),
        ).isNull()
        assertThat(
            incidentEntity(
                deletedAt = now,
                deletedBy = null,
            ).toDeleteRequest(),
        ).isNull()
    }
    // endregion

    // region PhotoEntity
    @Test
    fun `photo entities map to the domain`() {
        val entity =
            photoEntity(
                position = 2,
                localPath = "/files/incidents/incident-1/photos/photo-2.jpg",
                remoteUrl = null,
                uploadState = PhotoUploadState.PENDING,
            )
        val domain =
            photo(
                position = 2,
                localPath = "/files/incidents/incident-1/photos/photo-2.jpg",
                remoteUrl = null,
                uploadState = PhotoUploadState.PENDING,
            )

        assertThat(entity.toDomain()).isEqualTo(domain)
    }
    // endregion

    // region ReferenceEntities
    @Test
    fun `reference entities map to the domain`() {
        val entities = referenceDataDto().toEntities()

        assertThat(entities.sites.single().toDomain())
            .isEqualTo(
                Site(
                    id = "site-1",
                    name = "Head Office",
                ),
            )
        assertThat(entities.buildings.single().toDomain())
            .isEqualTo(
                Building(
                    id = "building-1",
                    siteId = "site-1",
                    name = "North Block",
                ),
            )
        assertThat(entities.floors.single().toDomain())
            .isEqualTo(
                Floor(
                    id = "floor-1",
                    buildingId = "building-1",
                    name = "Ground Floor",
                ),
            )
        assertThat(entities.rooms.single().toDomain())
            .isEqualTo(
                Room(
                    id = "room-1",
                    floorId = "floor-1",
                    name = "Lobby",
                ),
            )
        assertThat(entities.assets.single().toDomain())
            .isEqualTo(
                Asset(
                    id = "asset-1",
                    roomId = "room-1",
                    name = "Revolving Door",
                    code = "DOOR-001",
                ),
            )
    }
    // endregion

    private fun toDomain(entity: IncidentEntity) = IncidentWithPhotos(
        incident = entity,
        photos = emptyList(),
    ).toDomain()
}
