package com.jonesl7l.carinfg.app

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.MainDispatcherRule
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `no session is logged out`() = runTest {
        val viewModel = AppViewModel(sessionStore = FakeSessionStore())

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(AppUiState.LoggedOut)
        }
    }

    @Test
    fun `a session is logged in`() = runTest {
        val viewModel = AppViewModel(sessionStore = FakeSessionStore(initial = technician))

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(loggedIn(isSessionExpired = false))
        }
    }

    @Test
    fun `an expired session stays logged in and is flagged until the user signs in again`() = runTest {
        val sessionStore = FakeSessionStore(initial = technician)
        val viewModel = AppViewModel(sessionStore = sessionStore)

        viewModel.uiState.test {
            awaitItem()
            sessionStore.markSessionExpired()
            assertThat(awaitItem()).isEqualTo(loggedIn(isSessionExpired = true))
            sessionStore.save(user = technician)
            assertThat(awaitItem()).isEqualTo(loggedIn(isSessionExpired = false))
        }
    }

    @Test
    fun `logging out moves to logged out`() = runTest {
        val sessionStore = FakeSessionStore(initial = technician)
        val viewModel = AppViewModel(sessionStore = sessionStore)

        viewModel.uiState.test {
            awaitItem()
            sessionStore.clear()
            assertThat(awaitItem()).isEqualTo(AppUiState.LoggedOut)
        }
    }

    private fun loggedIn(isSessionExpired: Boolean) = AppUiState.LoggedIn(
        user = technician,
        isSessionExpired = isSessionExpired,
    )

    private companion object {
        val technician =
            User(
                id = "u1",
                username = "tech1",
                displayName = "Tech One",
                role = UserRole.Technician,
            )
    }
}
