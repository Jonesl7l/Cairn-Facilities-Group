package com.jonesl7l.carinfg.core.domain.policy

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.REPORTER_ID
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incident
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.operatorFields
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import org.junit.Test

class IncidentPolicyHelperTest {
    @Test
    fun `owner can edit and delete while open`() {
        val incident = incidentWithStatus(status = IncidentStatus.Open)

        assertThat(
            IncidentPolicyHelper.canEdit(
                incident = incident,
                userId = REPORTER_ID,
            ),
        ).isTrue()
        assertThat(
            IncidentPolicyHelper.canDelete(
                incident = incident,
                userId = REPORTER_ID,
            ),
        ).isTrue()
    }

    @Test
    fun `owner can edit but not delete while assigned`() {
        val incident = incidentWithStatus(status = IncidentStatus.Assigned)

        assertThat(
            IncidentPolicyHelper.canEdit(
                incident = incident,
                userId = REPORTER_ID,
            ),
        ).isTrue()
        assertThat(
            IncidentPolicyHelper.canDelete(
                incident = incident,
                userId = REPORTER_ID,
            ),
        ).isFalse()
    }

    @Test
    fun `owner cannot change an incident in progress`() {
        val incident = incidentWithStatus(status = IncidentStatus.InProgress)

        assertThat(
            IncidentPolicyHelper.canEdit(
                incident = incident,
                userId = REPORTER_ID,
            ),
        ).isFalse()
        assertThat(
            IncidentPolicyHelper.canDelete(
                incident = incident,
                userId = REPORTER_ID,
            ),
        ).isFalse()
    }

    @Test
    fun `nothing is allowed once resolved, closed or unknown`() {
        val statuses = listOf(
            IncidentStatus.Resolved,
            IncidentStatus.Closed,
            IncidentStatus.Unknown(raw = "ARCHIVED"),
        )
        statuses.forEach { status ->
            val incident = incidentWithStatus(status = status)

            assertThat(
                IncidentPolicyHelper.canEdit(
                    incident = incident,
                    userId = REPORTER_ID,
                ),
            ).isFalse()
            assertThat(
                IncidentPolicyHelper.canDelete(
                    incident = incident,
                    userId = REPORTER_ID,
                ),
            ).isFalse()
        }
    }

    @Test
    fun `other technicians cannot change an open incident`() {
        val incident = incidentWithStatus(status = IncidentStatus.Open)

        assertThat(
            IncidentPolicyHelper.canEdit(
                incident = incident,
                userId = "tech2",
            ),
        ).isFalse()
        assertThat(
            IncidentPolicyHelper.canDelete(
                incident = incident,
                userId = "tech2",
            ),
        ).isFalse()
    }

    private fun incidentWithStatus(status: IncidentStatus): Incident = incident(operatorFields = operatorFields(status = status))
}
