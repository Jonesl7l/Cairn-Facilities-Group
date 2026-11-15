package com.jonesl7l.carinfg.feature.incident.list

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentListItem
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSort
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentStatus
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.Test

class IncidentListHelperTest {
    @Test
    fun `mine shows only my incidents and team open shows others unresolved ones`() {
        val items =
            listOf(
                item(id = "mine"),
                item(
                    id = "othersOpen",
                    reporterId = OTHER_ID,
                ),
                item(
                    id = "othersResolved",
                    reporterId = OTHER_ID,
                    status = IncidentStatus.Resolved,
                ),
                item(
                    id = "othersClosed",
                    reporterId = OTHER_ID,
                    status = IncidentStatus.Closed,
                ),
            )

        assertThat(ids(sections(items = items).incidents)).containsExactly("mine")
        assertThat(
            ids(
                sections(
                    items = items,
                    scope = IncidentScope.TEAM_OPEN,
                ).incidents,
            ),
        ).containsExactly("othersOpen")
    }

    @Test
    fun `needs attention and drafts are pinned and ignore the status filter`() {
        val items =
            listOf(
                item(
                    id = "attention",
                    syncState = SyncState.NEEDS_ATTENTION,
                    status = IncidentStatus.Assigned,
                ),
                item(
                    id = "draft",
                    syncState = SyncState.DRAFT,
                ),
                item(id = "synced"),
            )

        val sections =
            sections(
                items = items,
                statusFilter = StatusFilter.RESOLVED,
            )

        assertThat(ids(sections.needsAttention)).containsExactly("attention")
        assertThat(ids(sections.drafts)).containsExactly("draft")
        assertThat(sections.incidents).isEmpty()
    }

    @Test
    fun `the status filter keeps matching incidents and resolved includes closed`() {
        val items =
            listOf(
                item(id = "open"),
                item(
                    id = "assigned",
                    status = IncidentStatus.Assigned,
                ),
                item(
                    id = "inProgress",
                    status = IncidentStatus.InProgress,
                ),
                item(
                    id = "resolved",
                    status = IncidentStatus.Resolved,
                ),
                item(
                    id = "closed",
                    status = IncidentStatus.Closed,
                ),
            )

        assertThat(
            ids(
                sections(
                    items = items,
                    statusFilter = StatusFilter.IN_PROGRESS,
                ).incidents,
            ),
        )
            .containsExactly("inProgress")
        assertThat(
            ids(
                sections(
                    items = items,
                    statusFilter = StatusFilter.RESOLVED,
                ).incidents,
            ),
        )
            .containsExactly("resolved", "closed")
    }

    @Test
    fun `newest sorts by occurred time, latest first`() {
        val items =
            listOf(
                item(
                    id = "old",
                    occurredAt = now - 3.hours,
                ),
                item(
                    id = "new",
                    occurredAt = now,
                ),
                item(
                    id = "middle",
                    occurredAt = now - 1.hours,
                ),
            )

        assertThat(ids(sections(items = items).incidents)).containsExactly("new", "middle", "old")
    }

    @Test
    fun `severity sorts critical to low, newest first within each`() {
        val items =
            listOf(
                item(
                    id = "lowNew",
                    severity = Severity.Low,
                ),
                item(
                    id = "criticalOld",
                    severity = Severity.Critical,
                    occurredAt = now - 2.hours,
                ),
                item(
                    id = "criticalNew",
                    severity = Severity.Critical,
                ),
                item(
                    id = "none",
                    severity = null,
                ),
            )

        val sorted = sections(
            items = items,
            sort = IncidentSort.SEVERITY,
        ).incidents

        assertThat(ids(sorted)).containsExactly("criticalNew", "criticalOld", "lowNew", "none")
    }

    @Test
    fun `a to z sorts by title ignoring case`() {
        val items =
            listOf(
                item(
                    id = "b",
                    title = "broken handrail",
                ),
                item(
                    id = "c",
                    title = "Cable exposed",
                ),
                item(
                    id = "a",
                    title = "Alarm fault",
                ),
            )

        assertThat(
            ids(
                sections(
                    items = items,
                    sort = IncidentSort.A_TO_Z,
                ).incidents,
            ),
        ).containsExactly("a", "b", "c")
    }

    @Test
    fun `each sync state maps to its chip and many attempts become still trying`() {
        assertThat(IncidentListHelper.syncChip(item = item(id = "synced"))).isNull()
        assertThat(
            IncidentListHelper.syncChip(
                item = item(
                    id = "draft",
                    syncState = SyncState.DRAFT,
                ),
            ),
        )
            .isEqualTo(SyncChip.DRAFT)
        assertThat(
            IncidentListHelper.syncChip(
                item = item(
                    id = "attention",
                    syncState = SyncState.NEEDS_ATTENTION,
                ),
            ),
        )
            .isEqualTo(SyncChip.NEEDS_ATTENTION)
        assertThat(
            IncidentListHelper.syncChip(
                item = item(
                    id = "pending",
                    syncState = SyncState.PENDING_UPDATE,
                ),
            ),
        )
            .isEqualTo(SyncChip.PENDING)
        assertThat(
            IncidentListHelper.syncChip(
                item = item(
                    id = "retrying",
                    syncState = SyncState.PENDING_CREATE,
                    syncAttempts = 8,
                ),
            ),
        )
            .isEqualTo(SyncChip.STILL_TRYING)
    }

    private fun sections(
        items: List<IncidentListItem>,
        scope: IncidentScope = IncidentScope.MINE,
        statusFilter: StatusFilter = StatusFilter.ALL,
        sort: IncidentSort = IncidentSort.NEWEST,
    ) = IncidentListHelper.sections(
        items = items,
        userId = USER_ID,
        scope = scope,
        statusFilter = statusFilter,
        sort = sort,
    )

    private fun ids(items: List<IncidentListItem>) = items.map { it.id }

    private fun item(
        id: String,
        reporterId: String = USER_ID,
        title: String = "Incident $id",
        status: IncidentStatus = IncidentStatus.Open,
        severity: Severity? = Severity.Medium,
        syncState: SyncState = SyncState.SYNCED,
        occurredAt: Instant = now,
        syncAttempts: Int = 0,
    ) = IncidentListItem(
        id = id,
        title = title,
        referenceNumber = null,
        reporterId = reporterId,
        status = status,
        severity = severity,
        syncState = syncState,
        syncAttempts = syncAttempts,
        occurredAt = occurredAt,
        localUpdatedAt = occurredAt,
        siteName = null,
        buildingName = null,
    )

    private companion object {
        const val USER_ID = "u1"
        const val OTHER_ID = "u2"
        val now: Instant = Instant.parse(input = "2026-11-15T12:00:00Z")
    }
}
