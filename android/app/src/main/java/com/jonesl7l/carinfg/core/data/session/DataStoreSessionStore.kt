package com.jonesl7l.carinfg.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jonesl7l.carinfg.core.data.mapper.WireValueMapper
import com.jonesl7l.carinfg.core.domain.model.user.User
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

class DataStoreSessionStore @Inject constructor(private val dataStore: DataStore<Preferences>) : SessionStore {
    override val session: Flow<User?> = dataStore.data
        .catch { error ->
            if (error !is IOException) throw error
            Timber.e(error, "session: could not read the session, treating it as logged out")
            emit(emptyPreferences())
        }.map(::toUser)

    override suspend fun save(user: User) {
        dataStore.edit { preferences ->
            preferences[userIdKey] = user.id
            preferences[usernameKey] = user.username
            preferences[displayNameKey] = user.displayName
            preferences[roleKey] = WireValueMapper.toWire(role = user.role)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(userIdKey)
            preferences.remove(usernameKey)
            preferences.remove(displayNameKey)
            preferences.remove(roleKey)
        }
    }

    private fun toUser(preferences: Preferences): User? {
        val id = preferences[userIdKey] ?: return null
        val username = preferences[usernameKey] ?: return null
        val displayName = preferences[displayNameKey] ?: return null
        val role = preferences[roleKey] ?: return null
        return User(
            id = id,
            username = username,
            displayName = displayName,
            role = WireValueMapper.toUserRole(raw = role),
        )
    }

    private companion object {
        val userIdKey = stringPreferencesKey("session_user_id")
        val usernameKey = stringPreferencesKey("session_username")
        val displayNameKey = stringPreferencesKey("session_display_name")
        val roleKey = stringPreferencesKey("session_role")
    }
}
