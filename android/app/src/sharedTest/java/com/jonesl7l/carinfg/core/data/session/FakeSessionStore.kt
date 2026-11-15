package com.jonesl7l.carinfg.core.data.session

import com.jonesl7l.carinfg.core.domain.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSessionStore(initial: User? = null) : SessionStore {
    private val state = MutableStateFlow(initial)
    private val expired = MutableStateFlow(false)

    override val session: StateFlow<User?> = state

    override val isSessionExpired: StateFlow<Boolean> = expired

    override suspend fun save(user: User) {
        state.value = user
        expired.value = false
    }

    override suspend fun markSessionExpired() {
        expired.value = true
    }

    override suspend fun clear() {
        state.value = null
        expired.value = false
    }
}
