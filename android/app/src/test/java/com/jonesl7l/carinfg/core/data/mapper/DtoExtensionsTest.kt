package com.jonesl7l.carinfg.core.data.mapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.database.EntityTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.photoEntity
import com.jonesl7l.carinfg.core.database.entity.reference.AssetEntity
import com.jonesl7l.carinfg.core.database.entity.reference.BuildingEntity
import com.jonesl7l.carinfg.core.database.entity.reference.FloorEntity
import com.jonesl7l.carinfg.core.database.entity.reference.ReferenceEntities
import com.jonesl7l.carinfg.core.database.entity.reference.RoomEntity
import com.jonesl7l.carinfg.core.database.entity.reference.SiteEntity
import com.jonesl7l.carinfg.core.database.entity.reference.TechnicianEntity
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.ApiTestHelper.INCIDENT_ID
import com.jonesl7l.carinfg.core.network.ApiTestHelper.REPORTER_ID
import com.jonesl7l.carinfg.core.network.ApiTestHelper.incidentDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.photoDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.referenceDataDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.userDto
import org.junit.Test

class DtoExtensionsTest {
    // region ReferenceDataDto
    @Test
    fun `every list and field is copied to its entity`() {
        val expected =
            ReferenceEntities(
                sites = listOf(
                    SiteEntity(
                        id = "site-1",
                        name = "Head Office",
                    ),
                ),
                buildings = listOf(
                    BuildingEntity(
                        id = "building-1",
                        siteId = "site-1",
                        name = "North Block",
                    ),
                ),
                floors = listOf(
                    FloorEntity(
                        id = "floor-1",
                        buildingId = "building-1",
                        name = "Ground Floor",
                    ),
                ),
                rooms = listOf(
                    RoomEntity(
                        id = "room-1",
                        floorId = "floor-1",
                        name = "Lobby",
                    ),
                ),
                assets =
                    listOf(
                        AssetEntity(
                            id = "asset-1",
                            roomId = "room-1",
                            name = "Revolving Door",
                            code = "DOOR-001",
                        ),
                    ),
                technicians = listOf(
                    TechnicianEntity(
                        id = "u1",
                        username = "tech1",
                        displayName = "Tech One",
                    ),
                ),
            )

        assertThat(referenceDataDto().toEntities()).isEqualTo(expected)
    }
    // endregion

    // region UserDto
    @Test
    fun `a user maps with its role`() {
        val expected =
            User(
                id = "u1",
                username = "tech1",
                displayName = "Tech One",
                role = UserRole.Technician,
            )

        assertThat(userDto().toDomain()).isEqualTo(expected)
    }

    @Test
    fun `an unknown role is kept`() {
        assertThat(userDto(role = "ADMIN").toDomain().role).isEqualTo(UserRole.Unknown(raw = "ADMIN"))
    }
    // endregion

    // region IncidentDto
    @Test
    fun `a pulled incident maps to a clean synced entity`() {
        val expected =
            incidentEntity(
                reporterId = REPORTER_ID,
                rootCause = "Leaking roof",
                correctiveActions = "Roof repair booked",
            )

        assertThat(incidentDto().toEntity()).isEqualTo(expected)
    }

    @Test
    fun `a pulled incident without optional fields maps them to empty`() {
        val dto =
            incidentDto(
                gpsLocation = null,
                buildingId = null,
                floorId = null,
                roomId = null,
                exactSpot = null,
                assetId = null,
                equipmentOutOfService = false,
                immediateActions = emptyList(),
                immediateActionsOther = null,
                persons = emptyList(),
                additionalNotes = null,
                assignedTo = null,
                dueDate = null,
                rootCause = null,
                correctiveActions = null,
                regulatoryReportable = null,
                supervisorAcknowledged = null,
            )

        val expected =
            incidentEntity(
                reporterId = REPORTER_ID,
                gpsLocation = null,
                buildingId = null,
                floorId = null,
                roomId = null,
                exactSpot = null,
                assetId = null,
                equipmentOutOfService = false,
                immediateActions = emptyList(),
                immediateActionsOther = null,
                persons = emptyList(),
                additionalNotes = null,
                assignedTo = null,
                dueDate = null,
                regulatoryReportable = null,
                supervisorAcknowledged = null,
            )
        assertThat(dto.toEntity()).isEqualTo(expected)
    }

    @Test
    fun `unknown values are kept raw and an unknown severity ranks last`() {
        val entity = incidentDto(
            severity = "SEVERE",
            status = "ON_HOLD",
        ).toEntity()

        assertThat(entity.severity).isEqualTo("SEVERE")
        assertThat(entity.severityRank).isEqualTo(0)
        assertThat(entity.status).isEqualTo("ON_HOLD")
    }

    @Test
    fun `pulled photos take their position from the array order`() {
        val dto = incidentDto(photos = listOf(photoDto(id = "photo-a"), photoDto(id = "photo-b")))

        val expected =
            listOf(
                photoEntity(
                    position = 0,
                    id = "photo-a",
                    remoteUrl = "uploads/incidents/$INCIDENT_ID/photo-a.jpg",
                ),
                photoEntity(
                    position = 1,
                    id = "photo-b",
                    remoteUrl = "uploads/incidents/$INCIDENT_ID/photo-b.jpg",
                ),
            )
        assertThat(dto.toPhotoEntities()).isEqualTo(expected)
    }
    // endregion
}
