package com.jonesl7l.carinfg.core.data.mapper

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.database.EntityTestHelper.draftIncidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.incidentEntity
import com.jonesl7l.carinfg.core.database.EntityTestHelper.now
import com.jonesl7l.carinfg.core.database.EntityTestHelper.personColumn
import com.jonesl7l.carinfg.core.database.EntityTestHelper.photoEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incident
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.incidentDetails
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.operatorFields
import com.jonesl7l.carinfg.core.domain.IncidentTestHelper.photo
import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentType
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.model.photo.PhotoUploadState
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class DomainExtensionsTest {
    // region Incident
    @Test
    fun `a full incident survives entity to domain and back`() {
        val entity =
            incidentEntity(
                serverVersion = 7,
                changeSeq = 99,
                localRevision = 3,
                previousSyncState = SyncState.SYNCED,
                undoDeadline = now + 5.seconds,
                syncState = SyncState.PENDING_DELETE,
            )

        assertThat(roundTrip(entity = entity)).isEqualTo(entity)
    }

    @Test
    fun `a draft with every optional field empty survives a round trip`() {
        val draft = draftIncidentEntity()

        assertThat(roundTrip(entity = draft)).isEqualTo(draft)
    }

    @Test
    fun `other and unknown values survive a round trip`() {
        val entity =
            incidentEntity(
                incidentType = "OTHER",
                incidentTypeOther = "Pigeon in the lift",
                severity = "SEVERE",
                severityRank = 0,
                status = "ON_HOLD",
                immediateActions = listOf("OTHER", "POWER_CUT"),
                immediateActionsOther = "Put up signs",
                persons = listOf(
                    personColumn(
                        personType = "VOLUNTEER",
                        role = "FIRST_AIDER",
                        injury = null,
                    ),
                ),
            )

        val domain = toDomain(entity = entity)

        assertThat(domain.details.type).isEqualTo(IncidentType.Other(description = "Pigeon in the lift"))
        assertThat(domain.details.severity).isEqualTo(Severity.Unknown(raw = "SEVERE"))
        assertThat(domain.operatorFields.status).isEqualTo(IncidentStatus.Unknown(raw = "ON_HOLD"))
        assertThat(domain.safety.immediateActions.toList())
            .containsExactly(
                ImmediateAction.Other(description = "Put up signs"),
                ImmediateAction.Unknown(raw = "POWER_CUT"),
            )
        assertThat(roundTrip(entity = entity)).isEqualTo(entity)
    }

    @Test
    fun `a new incident gets default sync columns and the given update time`() {
        val updatedAt = now + 1.hours

        val entity =
            incident(details = incidentDetails(severity = Severity.Critical)).toEntity(
                existing = null,
                localUpdatedAt = updatedAt,
            )

        assertThat(entity.serverVersion).isEqualTo(null)
        assertThat(entity.changeSeq).isEqualTo(null)
        assertThat(entity.localRevision).isEqualTo(0L)
        assertThat(entity.previousSyncState).isEqualTo(null)
        assertThat(entity.undoDeadline).isEqualTo(null)
        assertThat(entity.localUpdatedAt).isEqualTo(updatedAt)
        assertThat(entity.severityRank).isEqualTo(Severity.Critical.rank)
    }
    // endregion

    // region Photo
    @Test
    fun `photos map to entities`() {
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

        assertThat(domain.toEntity()).isEqualTo(entity)
    }
    // endregion

    private fun toDomain(entity: IncidentEntity) = IncidentWithPhotos(
        incident = entity,
        photos = emptyList(),
    ).toDomain()

    private fun roundTrip(entity: IncidentEntity): IncidentEntity {
        val domain = toDomain(entity = entity)
        return domain.toEntity(
            existing = entity,
            localUpdatedAt = entity.localUpdatedAt,
        )
    }
}
