package com.jonesl7l.carinfg.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSort
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreUserPreferencesStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `the sort is newest until one is chosen`() = runTest {
        assertThat(store(dataStore = dataStore()).incidentSort.first()).isEqualTo(IncidentSort.NEWEST)
    }

    @Test
    fun `a chosen sort is kept`() = runTest {
        val store = store(dataStore = dataStore())

        store.setIncidentSort(sort = IncidentSort.SEVERITY)

        assertThat(store.incidentSort.first()).isEqualTo(IncidentSort.SEVERITY)
    }

    @Test
    fun `an unknown stored sort falls back to newest`() = runTest {
        val dataStore = dataStore()
        dataStore.edit { it[stringPreferencesKey("incident_sort")] = "OLDEST" }

        assertThat(store(dataStore = dataStore).incidentSort.first()).isEqualTo(IncidentSort.NEWEST)
    }

    private fun TestScope.dataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { File(temporaryFolder.root, FILE_NAME) },
    )

    private fun store(dataStore: DataStore<Preferences>) = DataStoreUserPreferencesStore(dataStore = dataStore)

    private companion object {
        const val FILE_NAME = "preferences_test.preferences_pb"
    }
}
