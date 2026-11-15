package com.jonesl7l.carinfg.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSessionStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val user =
        User(
            id = "u1",
            username = "tech1",
            displayName = "Tech One",
            role = UserRole.Technician,
        )

    @Test
    fun `there is no session before a user is saved`() = runTest {
        assertThat(store(dataStore = dataStore()).session.first()).isNull()
    }

    @Test
    fun `a saved user comes back`() = runTest {
        val store = store(dataStore = dataStore())

        store.save(user = user)

        assertThat(store.session.first()).isEqualTo(user)
    }

    @Test
    fun `an unknown role is kept`() = runTest {
        val store = store(dataStore = dataStore())
        val admin = user.copy(role = UserRole.Unknown(raw = "ADMIN"))

        store.save(user = admin)

        assertThat(store.session.first()).isEqualTo(admin)
    }

    @Test
    fun `clear removes only the session`() = runTest {
        val dataStore = dataStore()
        val store = store(dataStore = dataStore)
        val otherKey = stringPreferencesKey("other_setting")
        store.save(user = user)
        dataStore.edit { it[otherKey] = "kept" }

        store.clear()

        assertThat(store.session.first()).isNull()
        assertThat(dataStore.data.first()[otherKey]).isEqualTo("kept")
    }

    private fun TestScope.dataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { File(temporaryFolder.root, FILE_NAME) },
    )

    private fun store(dataStore: DataStore<Preferences>) = DataStoreSessionStore(dataStore = dataStore)

    private companion object {
        const val FILE_NAME = "session_test.preferences_pb"
    }
}
