package com.jonesl7l.carinfg.core.database

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider

object DatabaseTestHelper {
    fun inMemoryDatabase(): AppDatabase = Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .build()
}
