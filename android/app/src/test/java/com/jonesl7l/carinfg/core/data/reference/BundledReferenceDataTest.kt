package com.jonesl7l.carinfg.core.data.reference

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.containsOnly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import com.jonesl7l.carinfg.core.network.ApiTestHelper.json
import com.jonesl7l.carinfg.core.network.dto.reference.ReferenceDataDto
import java.io.File
import org.junit.Test

class BundledReferenceDataTest {
    private val data = json.decodeFromString<ReferenceDataDto>(File(ASSET_PATH).readText())

    @Test
    fun `the bundled file decodes with the seed sizes`() {
        assertThat(data.sites).hasSize(2)
        assertThat(data.buildings).hasSize(5)
        assertThat(data.assets).hasSize(11)
        assertThat(data.technicians.map { it.username }).containsExactlyInAnyOrder("tech1", "tech2")
        assertThat(data.technicians.map { it.role }.toSet()).containsOnly("TECHNICIAN")
    }

    @Test
    fun `ids are unique within each list`() {
        val idLists =
            listOf(
                data.sites.map { it.id },
                data.buildings.map { it.id },
                data.floors.map { it.id },
                data.rooms.map { it.id },
                data.assets.map { it.id },
                data.technicians.map { it.id },
            )

        idLists.forEach { ids -> assertThat(ids.groupBy { it }.filterValues { it.size > 1 }.keys).isEmpty() }
    }

    @Test
    fun `every child points at a parent that exists`() {
        val siteIds = data.sites.map { it.id }.toSet()
        val buildingIds = data.buildings.map { it.id }.toSet()
        val floorIds = data.floors.map { it.id }.toSet()
        val roomIds = data.rooms.map { it.id }.toSet()

        assertThat(data.buildings.filterNot { it.siteId in siteIds }).isEmpty()
        assertThat(data.floors.filterNot { it.buildingId in buildingIds }).isEmpty()
        assertThat(data.rooms.filterNot { it.floorId in floorIds }).isEmpty()
        assertThat(data.assets.filterNot { it.roomId in roomIds }).isEmpty()
    }

    private companion object {
        const val ASSET_PATH = "src/main/assets/${ReferenceDataInitializer.ASSET_NAME}"
    }
}
