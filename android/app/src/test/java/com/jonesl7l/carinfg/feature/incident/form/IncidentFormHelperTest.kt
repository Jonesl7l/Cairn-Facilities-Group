package com.jonesl7l.carinfg.feature.incident.form

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incident
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incidentLocation
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.injuryDetails
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.now
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.person
import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentLocation
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSafety
import com.jonesl7l.carinfg.core.domain.model.person.InjuryDetails
import com.jonesl7l.carinfg.core.domain.model.person.Person
import com.jonesl7l.carinfg.core.domain.model.person.PersonRole
import com.jonesl7l.carinfg.core.domain.model.person.PersonType
import com.jonesl7l.carinfg.core.domain.model.reference.Building
import com.jonesl7l.carinfg.core.domain.model.reference.Site
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import kotlinx.datetime.TimeZone
import org.junit.Test

class IncidentFormHelperTest {
    private val fields = incident().toFormFields()

    // region location
    @Test
    fun `changing the site clears everything below it`() {
        val next = fields.copy(location = fields.location.copy(siteId = "site-2"))

        val result = applyRules(next = next).location

        assertThat(result).isEqualTo(
            fields.location.copy(
                siteId = "site-2",
                buildingId = null,
                floorId = null,
                roomId = null,
                assetId = null,
                equipmentOutOfService = false,
            ),
        )
    }

    @Test
    fun `changing the room clears the asset and the out of service flag`() {
        val previous = fields.copy(
            location = incidentLocation(
                roomId = "room-1",
                assetId = "asset-1",
                equipmentOutOfService = true,
            ),
        )
        val next = previous.copy(location = previous.location.copy(roomId = "room-2"))

        val result = IncidentFormHelper.applyRules(
            previous = previous,
            next = next,
        ).location

        assertThat(result.assetId).isNull()
        assertThat(result.equipmentOutOfService).isFalse()
    }

    @Test
    fun `choosing a building clears the unlisted location`() {
        val previous = fields.copy(
            location = IncidentLocation(
                siteId = "site-1",
                unlistedLocation = "Bike shed",
            ),
        )
        val next = previous.copy(location = previous.location.copy(buildingId = "building-1"))

        val result = IncidentFormHelper.applyRules(
            previous = previous,
            next = next,
        ).location

        assertThat(result.unlistedLocation).isNull()
    }
    // endregion

    // region immediate actions
    @Test
    fun `choosing none clears the other actions`() {
        val previous = withActions(ImmediateAction.AreaCordonedOff)
        val next = withActions(
            ImmediateAction.AreaCordonedOff,
            ImmediateAction.None,
        )

        val result = IncidentFormHelper.applyRules(
            previous = previous,
            next = next,
        )

        assertThat(result.safety.immediateActions).isEqualTo(setOf(ImmediateAction.None))
    }

    @Test
    fun `choosing another action clears none`() {
        val previous = withActions(ImmediateAction.None)
        val next = withActions(
            ImmediateAction.None,
            ImmediateAction.FirstAidGiven,
        )

        val result = IncidentFormHelper.applyRules(
            previous = previous,
            next = next,
        )

        assertThat(result.safety.immediateActions).isEqualTo(setOf(ImmediateAction.FirstAidGiven))
    }
    // endregion

    // region persons
    @Test
    fun `employee and contractor fields only stay for that person type`() {
        val visitor = person(
            type = PersonType.Visitor,
            department = "Facilities",
            position = "Cleaner",
            company = "Acme",
        )

        val result = applyRules(next = fields.copy(persons = listOf(visitor))).persons.single()

        assertThat(result.department).isNull()
        assertThat(result.position).isNull()
        assertThat(result.company).isNull()
    }

    @Test
    fun `injury details only exist for an injured person`() {
        val injured = person(
            role = PersonRole.Injured,
            injury = null,
        )
        val witness = person(
            role = PersonRole.Witness,
            injury = injuryDetails(),
        )

        val result = applyRules(next = fields.copy(persons = listOf(injured, witness))).persons

        assertThat(result[0].injury).isEqualTo(InjuryDetails())
        assertThat(result[1].injury).isNull()
    }

    @Test
    fun `a person can be added when there are none or the last one has data`() {
        assertThat(IncidentFormHelper.canAddPerson(persons = emptyList())).isTrue()
        assertThat(IncidentFormHelper.canAddPerson(persons = listOf(Person(id = "person-1")))).isFalse()
        assertThat(
            IncidentFormHelper.canAddPerson(
                persons = listOf(
                    Person(
                        id = "person-1",
                        name = "Sam",
                    ),
                ),
            ),
        ).isTrue()
        assertThat(
            IncidentFormHelper.canAddPerson(
                persons = listOf(
                    Person(
                        id = "person-1",
                        role = PersonRole.Witness,
                    ),
                ),
            ),
        ).isTrue()
    }
    // endregion

    @Test
    fun `options only list the children of the current selection`() {
        val references = ReferenceOptions(
            sites = listOf(
                Site(
                    id = "site-1",
                    name = "Head Office",
                ),
            ),
            buildings = listOf(
                Building(
                    id = "building-1",
                    siteId = "site-1",
                    name = "North Block",
                ),
                Building(
                    id = "building-2",
                    siteId = "site-2",
                    name = "Workshop",
                ),
            ),
        )

        val options = IncidentFormHelper.optionsFor(
            references = references,
            location = IncidentLocation(siteId = "site-1"),
        )

        assertThat(options.sites).isEqualTo(references.sites)
        assertThat(options.buildings.map { it.id }).isEqualTo(listOf("building-1"))
    }

    @Test
    fun `a new incident is an empty draft that occurred now`() {
        val incident = IncidentFormHelper.newIncident(
            id = "incident-2",
            reporterId = "tech1",
            now = now,
            timeZone = TimeZone.UTC,
        )

        assertThat(incident.syncStatus.state).isEqualTo(SyncState.DRAFT)
        assertThat(incident.details.occurredAt).isEqualTo(now)
        assertThat(incident.details.title).isEqualTo("")
        assertThat(incident.safety).isEqualTo(IncidentSafety())
    }

    @Test
    fun `form fields round trip onto the incident`() {
        val incident = incident()

        assertThat(incident.toFormFields().applyTo(incident = incident)).isEqualTo(incident)
    }

    private fun applyRules(next: IncidentFormFields) = IncidentFormHelper.applyRules(
        previous = fields,
        next = next,
    )

    private fun withActions(vararg actions: ImmediateAction) = fields.copy(
        safety = fields.safety.copy(immediateActions = actions.toSet()),
    )
}
