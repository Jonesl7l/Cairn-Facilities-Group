package com.jonesl7l.carinfg.core.data.incident

import androidx.room3.withWriteTransaction
import com.jonesl7l.carinfg.core.data.mapper.toDomain
import com.jonesl7l.carinfg.core.data.mapper.toEntity
import com.jonesl7l.carinfg.core.data.session.SessionStore
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.dao.IncidentDao
import com.jonesl7l.carinfg.core.database.dao.ReferenceDao
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentDetail
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentListItem
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentNames
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.policy.IncidentPolicyHelper
import com.jonesl7l.carinfg.core.domain.validation.IncidentValidator
import com.jonesl7l.carinfg.core.sync.SyncScheduler
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class IncidentRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val incidentDao: IncidentDao,
    private val referenceDao: ReferenceDao,
    private val sessionStore: SessionStore,
    private val syncScheduler: SyncScheduler,
) : IncidentRepository {
    override fun observeIncident(id: String): Flow<Incident?> = incidentDao.observeIncident(id = id)
        .map { it?.toDomain() }
        .distinctUntilChanged()

    override fun observeIncidentDetail(id: String): Flow<IncidentDetail?> = combine(
        observeIncident(id = id),
        observeReferenceLookup(),
    ) { incident, lookup ->
        incident?.let {
            IncidentDetail(
                incident = it,
                names = lookup.namesFor(incident = it),
            )
        }
    }

    override fun observeIncidentList(): Flow<List<IncidentListItem>> = incidentDao
        .observeIncidentList()
        .map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveDraft(
        incident: Incident,
        now: Instant,
    ): SaveResult = database.withWriteTransaction {
        val stored = incidentDao.getIncident(id = incident.id)?.incident
        val draft =
            IncidentTransitionHelper.saveDraft(
                entity = incident.mergeOnto(
                    stored = stored,
                    now = now,
                ),
            )
                ?: return@withWriteTransaction SaveResult.NotAllowed
        incidentDao.upsertIncident(incident = draft)
        SaveResult.Saved
    }

    override suspend fun submit(
        incident: Incident,
        now: Instant,
    ): SaveResult {
        val errors = IncidentValidator.validate(
            incident = incident,
            now = now,
        )
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors = errors)
        val result = database.withWriteTransaction {
            val stored = incidentDao.getIncident(id = incident.id)?.incident
            val submitted =
                IncidentTransitionHelper.submit(
                    entity = incident.mergeOnto(
                        stored = stored,
                        now = now,
                    ),
                    now = now,
                ) ?: return@withWriteTransaction SaveResult.NotAllowed
            incidentDao.upsertIncident(incident = submitted)
            SaveResult.Saved
        }
        if (result == SaveResult.Saved) syncScheduler.requestSync()
        return result
    }

    override suspend fun saveEdit(
        incident: Incident,
        now: Instant,
    ): SaveResult {
        val userId = currentUserId() ?: return SaveResult.NotAllowed
        val errors = IncidentValidator.validate(
            incident = incident,
            now = now,
        )
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors = errors)
        val result = database.withWriteTransaction {
            val stored = incidentDao.getIncident(id = incident.id) ?: return@withWriteTransaction SaveResult.NotAllowed
            val canEdit =
                IncidentPolicyHelper.canEdit(
                    incident = stored.toDomain(),
                    userId = userId,
                )
            if (!canEdit) {
                return@withWriteTransaction SaveResult.NotAllowed
            }
            val edited =
                IncidentTransitionHelper.edit(
                    entity = incident.mergeOnto(
                        stored = stored.incident,
                        now = now,
                    ),
                )
                    ?: return@withWriteTransaction SaveResult.NotAllowed
            incidentDao.upsertIncident(incident = edited)
            SaveResult.Saved
        }
        if (result == SaveResult.Saved) syncScheduler.requestSync()
        return result
    }

    override suspend fun delete(
        id: String,
        now: Instant,
        undoWindow: Duration,
    ): DeleteResult {
        val userId = currentUserId() ?: return DeleteResult.NotAllowed
        return database.withWriteTransaction {
            val stored = incidentDao.getIncident(id = id) ?: return@withWriteTransaction DeleteResult.NotAllowed
            val canDelete =
                IncidentPolicyHelper.canDelete(
                    incident = stored.toDomain(),
                    userId = userId,
                )
            if (!canDelete) {
                return@withWriteTransaction DeleteResult.NotAllowed
            }
            val transition =
                IncidentTransitionHelper.delete(
                    entity = stored.incident.copy(localUpdatedAt = now),
                    now = now,
                    undoWindow = undoWindow,
                )
            when (transition) {
                DeleteTransition.Remove -> {
                    incidentDao.deleteIncident(id = id)
                    DeleteResult.Removed
                }

                is DeleteTransition.MarkPending -> {
                    incidentDao.upsertIncident(incident = transition.entity)
                    DeleteResult.PendingDelete(undoDeadline = now + undoWindow)
                }

                DeleteTransition.NotAllowed -> DeleteResult.NotAllowed
            }
        }
    }

    override suspend fun undoDelete(
        id: String,
        now: Instant,
    ): Boolean = database.withWriteTransaction {
        val stored = incidentDao.getIncident(id = id)?.incident ?: return@withWriteTransaction false
        val restored =
            IncidentTransitionHelper.undoDelete(entity = stored.copy(localUpdatedAt = now))
                ?: return@withWriteTransaction false
        incidentDao.upsertIncident(incident = restored)
        true
    }

    override suspend fun sendDeleteNow(
        id: String,
        now: Instant,
    ) {
        val sent =
            database.withWriteTransaction {
                val stored = incidentDao.getIncident(id = id)?.incident ?: return@withWriteTransaction false
                val entity =
                    IncidentTransitionHelper.sendDeleteNow(
                        entity = stored,
                        now = now,
                    )
                        ?: return@withWriteTransaction false
                incidentDao.upsertIncident(incident = entity)
                true
            }
        if (sent) syncScheduler.requestSync()
    }

    private fun observeReferenceLookup(): Flow<ReferenceLookup> = combine(
        referenceDao.observeSites(),
        referenceDao.observeAllBuildings(),
        referenceDao.observeAllFloors(),
        referenceDao.observeAllRooms(),
        referenceDao.observeAllAssets(),
    ) { sites, buildings, floors, rooms, assets ->
        ReferenceLookup(
            sites = sites.associate { it.id to it.name },
            buildings = buildings.associate { it.id to it.name },
            floors = floors.associate { it.id to it.name },
            rooms = rooms.associate { it.id to it.name },
            assets = assets.associateBy { it.id },
        )
    }.combine(referenceDao.observeTechnicians()) { lookup, technicians ->
        lookup.copy(technicians = technicians.associate { it.id to it.displayName })
    }

    private fun ReferenceLookup.namesFor(incident: Incident): IncidentNames {
        val location = incident.location
        val asset = location.assetId?.let { assets[it] }
        return IncidentNames(
            siteName = location.siteId?.let { sites[it] },
            buildingName = location.buildingId?.let { buildings[it] },
            floorName = location.floorId?.let { floors[it] },
            roomName = location.roomId?.let { rooms[it] },
            assetName = asset?.name,
            assetCode = asset?.code,
            reporterName = technicians[incident.reporterId],
        )
    }

    private suspend fun currentUserId(): String? = sessionStore.session.first()?.id

    private fun Incident.mergeOnto(
        stored: IncidentEntity?,
        now: Instant,
    ): IncidentEntity {
        val fromForm = toEntity(
            existing = stored,
            localUpdatedAt = now,
        )
        if (stored == null) return fromForm.copy(syncState = SyncState.DRAFT)
        return fromForm.copy(
            referenceNumber = stored.referenceNumber,
            status = stored.status,
            assignedTo = stored.assignedTo,
            dueDate = stored.dueDate,
            rootCause = stored.rootCause,
            correctiveActions = stored.correctiveActions,
            regulatoryReportable = stored.regulatoryReportable,
            supervisorAcknowledged = stored.supervisorAcknowledged,
            createdAt = stored.createdAt,
            updatedAt = stored.updatedAt,
            deletedAt = stored.deletedAt,
            deletedBy = stored.deletedBy,
            syncState = stored.syncState,
            conflictReason = stored.conflictReason,
            lastSyncError = stored.lastSyncError,
            syncAttempts = stored.syncAttempts,
        )
    }
}
