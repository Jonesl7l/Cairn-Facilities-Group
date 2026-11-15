package com.jonesl7l.carinfg.core.data.reference

import com.jonesl7l.carinfg.core.data.mapper.toDomain
import com.jonesl7l.carinfg.core.data.mapper.toEntities
import com.jonesl7l.carinfg.core.database.dao.ReferenceDao
import com.jonesl7l.carinfg.core.di.ApplicationScope
import com.jonesl7l.carinfg.core.domain.model.reference.Asset
import com.jonesl7l.carinfg.core.domain.model.reference.Building
import com.jonesl7l.carinfg.core.domain.model.reference.Floor
import com.jonesl7l.carinfg.core.domain.model.reference.Room
import com.jonesl7l.carinfg.core.domain.model.reference.Site
import com.jonesl7l.carinfg.core.network.datasource.ReferenceRemoteDataSource
import com.jonesl7l.carinfg.core.network.result.ApiResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class ReferenceRepository @Inject constructor(
    private val referenceDao: ReferenceDao,
    private val referenceRemoteDataSource: ReferenceRemoteDataSource,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private var refreshJob: Job? = null

    fun observeSites(): Flow<List<Site>> = referenceDao.observeSites()
        .map { sites -> sites.map { it.toDomain() } }
        .distinctUntilChanged()

    fun observeBuildings(siteId: String): Flow<List<Building>> = referenceDao.observeBuildings(siteId = siteId)
        .map { buildings -> buildings.map { it.toDomain() } }
        .distinctUntilChanged()

    fun observeFloors(buildingId: String): Flow<List<Floor>> = referenceDao.observeFloors(buildingId = buildingId)
        .map { floors -> floors.map { it.toDomain() } }
        .distinctUntilChanged()

    fun observeRooms(floorId: String): Flow<List<Room>> = referenceDao.observeRooms(floorId = floorId)
        .map { rooms -> rooms.map { it.toDomain() } }
        .distinctUntilChanged()

    fun observeAssets(roomId: String): Flow<List<Asset>> = referenceDao.observeAssets(roomId = roomId)
        .map { assets -> assets.map { it.toDomain() } }
        .distinctUntilChanged()

    @Synchronized
    fun refresh(): Job {
        refreshJob?.takeIf { it.isActive }?.let { return it }
        return applicationScope.launch {
            val result = referenceRemoteDataSource.getReferenceData()
            if (result is ApiResult.Success) referenceDao.replaceAll(entities = result.value.toEntities())
        }.also { refreshJob = it }
    }
}
