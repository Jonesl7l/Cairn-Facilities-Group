package com.jonesl7l.carinfg.core.data.session

import com.jonesl7l.carinfg.core.domain.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSessionStore(initial: User? = null) : SessionStore {
    private val state = MutableStateFlow(initial)

    override val session: StateFlow<User?> = state

    override suspend fun save(user: User) {
        state.value = user
    }

    override suspend fun clear() {
        state.value = null
    }
}
