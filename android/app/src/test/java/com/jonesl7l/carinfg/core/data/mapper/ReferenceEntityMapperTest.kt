package com.jonesl7l.carinfg.core.data.mapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.database.entity.reference.AssetEntity
import com.jonesl7l.carinfg.core.database.entity.reference.BuildingEntity
import com.jonesl7l.carinfg.core.database.entity.reference.FloorEntity
import com.jonesl7l.carinfg.core.database.entity.reference.ReferenceEntities
import com.jonesl7l.carinfg.core.database.entity.reference.RoomEntity
import com.jonesl7l.carinfg.core.database.entity.reference.SiteEntity
import com.jonesl7l.carinfg.core.database.entity.reference.TechnicianEntity
import com.jonesl7l.carinfg.core.network.ApiTestHelper.referenceDataDto
import org.junit.Test

class ReferenceEntityMapperTest {
    @Test
    fun `every list and field is copied to its entity`() {
        val expected =
            ReferenceEntities(
                sites = listOf(SiteEntity(id = "site-1", name = "Head Office")),
                buildings = listOf(BuildingEntity(id = "building-1", siteId = "site-1", name = "North Block")),
                floors = listOf(FloorEntity(id = "floor-1", buildingId = "building-1", name = "Ground Floor")),
                rooms = listOf(RoomEntity(id = "room-1", floorId = "floor-1", name = "Lobby")),
                assets =
                    listOf(
                        AssetEntity(
                            id = "asset-1",
                            roomId = "room-1",
                            name = "Revolving Door",
                            code = "DOOR-001",
                        ),
                    ),
                technicians = listOf(TechnicianEntity(id = "u1", username = "tech1", displayName = "Tech One")),
            )

        assertThat(ReferenceEntityMapper.toEntities(dto = referenceDataDto())).isEqualTo(expected)
    }
}
