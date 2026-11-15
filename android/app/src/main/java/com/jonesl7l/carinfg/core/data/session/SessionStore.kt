package com.jonesl7l.carinfg.core.data.session

import com.jonesl7l.carinfg.core.domain.model.user.User
import kotlinx.coroutines.flow.Flow

interface SessionStore {
    val session: Flow<User?>

    val isSessionExpired: Flow<Boolean>

    suspend fun save(user: User)

    suspend fun markSessionExpired()

    suspend fun clear()
}
