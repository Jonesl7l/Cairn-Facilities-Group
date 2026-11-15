package com.jonesl7l.carinfg.core.data.reference

import android.content.Context
import com.jonesl7l.carinfg.core.data.mapper.ReferenceEntityMapper
import com.jonesl7l.carinfg.core.database.dao.ReferenceDao
import com.jonesl7l.carinfg.core.di.IoDispatcher
import com.jonesl7l.carinfg.core.network.dto.reference.ReferenceDataDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import timber.log.Timber

class ReferenceDataInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val referenceDao: ReferenceDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun initialize() {
        withContext(ioDispatcher) {
            // skips reading the asset on every launch; insertAllIfEmpty still guards the race
            if (!referenceDao.isEmpty()) return@withContext
            val dto = readBundledData() ?: return@withContext
            referenceDao.insertAllIfEmpty(entities = ReferenceEntityMapper.toEntities(dto = dto))
        }
    }

    private fun readBundledData(): ReferenceDataDto? = try {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        json.decodeFromString<ReferenceDataDto>(text)
    } catch (error: IOException) {
        Timber.e(error, "readBundledData: could not read %s", ASSET_NAME)
        null
    } catch (error: SerializationException) {
        Timber.e(error, "readBundledData: could not decode %s", ASSET_NAME)
        null
    }

    companion object {
        const val ASSET_NAME = "reference.json"
    }
}
