package com.jonesl7l.carinfg.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSort
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

class DataStoreUserPreferencesStore @Inject constructor(private val dataStore: DataStore<Preferences>) :
    UserPreferencesStore {
    override val incidentSort: Flow<IncidentSort> = dataStore.data
        .catch { error ->
            if (error !is IOException) throw error
            Timber.e(error, "incidentSort: could not read preferences, using the default sort")
            emit(emptyPreferences())
        }.map { preferences ->
            IncidentSort.entries.firstOrNull { it.name == preferences[incidentSortKey] } ?: IncidentSort.NEWEST
        }

    override suspend fun setIncidentSort(sort: IncidentSort) {
        dataStore.edit { preferences -> preferences[incidentSortKey] = sort.name }
    }

    private companion object {
        val incidentSortKey = stringPreferencesKey("incident_sort")
    }
}
