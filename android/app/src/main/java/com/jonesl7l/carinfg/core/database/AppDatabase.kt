package com.jonesl7l.carinfg.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.jonesl7l.carinfg.core.database.dao.ReferenceDao
import com.jonesl7l.carinfg.core.database.entity.reference.AssetEntity
import com.jonesl7l.carinfg.core.database.entity.reference.BuildingEntity
import com.jonesl7l.carinfg.core.database.entity.reference.FloorEntity
import com.jonesl7l.carinfg.core.database.entity.reference.RoomEntity
import com.jonesl7l.carinfg.core.database.entity.reference.SiteEntity
import com.jonesl7l.carinfg.core.database.entity.reference.TechnicianEntity

@Database(
    entities = [
        SiteEntity::class,
        BuildingEntity::class,
        FloorEntity::class,
        RoomEntity::class,
        AssetEntity::class,
        TechnicianEntity::class,
    ],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun referenceDao(): ReferenceDao
}
