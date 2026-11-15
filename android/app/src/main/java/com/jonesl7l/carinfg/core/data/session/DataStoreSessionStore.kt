package com.jonesl7l.carinfg.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    private val preferences: Flow<Preferences> = dataStore.data
        .catch { error ->
            if (error !is IOException) throw error
            Timber.e(error, "preferences: could not read the session, treating it as logged out")
            emit(emptyPreferences())
        }

    override val session: Flow<User?> = preferences.map(::toUser)

    override val isSessionExpired: Flow<Boolean> = preferences.map { it[sessionExpiredKey] ?: false }

    override suspend fun save(user: User) {
        dataStore.edit { preferences ->
            preferences[userIdKey] = user.id
            preferences[usernameKey] = user.username
            preferences[displayNameKey] = user.displayName
            preferences[roleKey] = WireValueMapper.toWire(role = user.role)
            preferences.remove(sessionExpiredKey)
        }
    }

    override suspend fun markSessionExpired() {
        dataStore.edit { preferences -> preferences[sessionExpiredKey] = true }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(userIdKey)
            preferences.remove(usernameKey)
            preferences.remove(displayNameKey)
            preferences.remove(roleKey)
            preferences.remove(sessionExpiredKey)
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
        val sessionExpiredKey = booleanPreferencesKey("session_expired")
    }
}
