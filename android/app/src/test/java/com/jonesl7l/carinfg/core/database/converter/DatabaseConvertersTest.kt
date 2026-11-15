package com.jonesl7l.carinfg.core.database.converter

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.jonesl7l.carinfg.core.database.entity.incident.InjuryColumn
import com.jonesl7l.carinfg.core.database.entity.incident.PersonColumn
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.junit.Test

class DatabaseConvertersTest {
    private val converters = DatabaseConverters()

    @Test
    fun `instants round trip through epoch millis`() {
        val instant = Instant.parse(input = "2026-11-13T12:00:00.123Z")

        val stored = converters.instantToEpochMillis(value = instant)

        assertThat(converters.epochMillisToInstant(value = stored)).isEqualTo(instant)
    }

    @Test
    fun `null instants and dates stay null`() {
        assertThat(converters.instantToEpochMillis(value = null)).isNull()
        assertThat(converters.epochMillisToInstant(value = null)).isNull()
        assertThat(converters.localDateToIso(value = null)).isNull()
        assertThat(converters.isoToLocalDate(value = null)).isNull()
    }

    @Test
    fun `dates are stored as iso strings`() {
        val date = LocalDate(
            year = 2026,
            month = 11,
            day = 20,
        )

        val stored = converters.localDateToIso(value = date)

        assertThat(stored).isEqualTo("2026-11-20")
        assertThat(converters.isoToLocalDate(value = stored)).isEqualTo(date)
    }

    @Test
    fun `string lists round trip, unknown values included`() {
        val actions = listOf("FIRST_AID_GIVEN", "SOMETHING_NEW")

        val stored = converters.stringsToJson(value = actions)

        assertThat(converters.jsonToStrings(value = stored)).isEqualTo(actions)
    }

    @Test
    fun `persons round trip with and without an injury`() {
        val injury =
            InjuryColumn(
                nature = "Sprain",
                bodyPart = "Left ankle",
                treatment = "FIRST_AID",
            )
        val persons =
            listOf(
                PersonColumn(
                    id = "person-1",
                    name = "Alex Smith",
                    personType = "EMPLOYEE",
                    role = "INJURED",
                    department = "Facilities",
                    injury = injury,
                ),
                PersonColumn(
                    id = "person-2",
                    name = "Sam Lee",
                    personType = "VISITOR",
                    role = "WITNESS",
                ),
            )

        val stored = converters.personsToJson(value = persons)

        assertThat(converters.jsonToPersons(value = stored)).isEqualTo(persons)
    }

    @Test
    fun `stored persons missing optional fields still decode`() {
        val stored = """[{"id":"person-1","name":"Alex","personType":"EMPLOYEE","role":"WITNESS","extra":1}]"""

        val expected =
            PersonColumn(
                id = "person-1",
                name = "Alex",
                personType = "EMPLOYEE",
                role = "WITNESS",
            )
        assertThat(converters.jsonToPersons(value = stored)).isEqualTo(listOf(expected))
    }
}
