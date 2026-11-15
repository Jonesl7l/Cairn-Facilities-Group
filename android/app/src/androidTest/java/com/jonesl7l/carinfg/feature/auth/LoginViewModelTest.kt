package com.jonesl7l.carinfg.feature.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.MainDispatcherRule
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.data.session.SessionRepository
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.DatabaseTestHelper.inMemoryDatabase
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.datasource.FakeAuthRemoteDataSource
import com.jonesl7l.carinfg.core.network.dto.user.UserDto
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import com.jonesl7l.carinfg.core.sync.FakeSyncScheduler
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val database: AppDatabase = inMemoryDatabase()
    private val sessionStore = FakeSessionStore()
    private val authRemoteDataSource = FakeAuthRemoteDataSource(result = ApiResult.Success(value = technicianDto))

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun logInIsOnlyPossibleWithAUsernameAndPassword() {
        val viewModel = viewModel()

        assertThat(viewModel.uiState.value.canLogIn).isFalse()
        viewModel.onUsernameChange(username = "tech1")
        assertThat(viewModel.uiState.value.canLogIn).isFalse()
        viewModel.onPasswordChange(password = "password")
        assertThat(viewModel.uiState.value.canLogIn).isTrue()
    }

    @Test
    fun aSuccessfulLogInSavesTheSessionAndKeepsTheSpinner() = runTest {
        val viewModel = filledIn()

        viewModel.logIn()

        assertThat(sessionStore.session.first()).isEqualTo(technician)
        assertThat(viewModel.uiState.value.isLoggingIn).isTrue()
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun wrongCredentialsShowTheirError() {
        authRemoteDataSource.result = ApiResult.Failure(error = ApiError.InvalidCredentials)

        assertThat(errorAfterLogIn()).isEqualTo(LoginError.INVALID_CREDENTIALS)
    }

    @Test
    fun anOperatorIsTurnedAway() {
        authRemoteDataSource.result = ApiResult.Success(value = technicianDto.copy(role = "OPERATOR"))

        assertThat(errorAfterLogIn()).isEqualTo(LoginError.NOT_TECHNICIAN)
    }

    @Test
    fun noConnectionAsksToGoOnline() {
        authRemoteDataSource.result = ApiResult.Failure(error = ApiError.Network(cause = IOException()))

        assertThat(errorAfterLogIn()).isEqualTo(LoginError.OFFLINE)
    }

    @Test
    fun otherFailuresAskToTryAgain() {
        authRemoteDataSource.result = ApiResult.Failure(error = ApiError.Server(status = 500))

        assertThat(errorAfterLogIn()).isEqualTo(LoginError.FAILED)
    }

    @Test
    fun editingAFieldClearsTheError() {
        authRemoteDataSource.result = ApiResult.Failure(error = ApiError.InvalidCredentials)
        val viewModel = filledIn()
        viewModel.logIn()

        viewModel.onPasswordChange(password = "another")

        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun anExpiredSessionLocksTheUsername() = runTest {
        sessionStore.save(user = technician)
        sessionStore.markSessionExpired()
        val viewModel = viewModel()

        viewModel.onUsernameChange(username = "tech2")

        assertThat(viewModel.uiState.value.username).isEqualTo("tech1")
        assertThat(viewModel.uiState.value.isUsernameLocked).isTrue()
    }

    private fun viewModel() = LoginViewModel(
        sessionRepository =
            SessionRepository(
                authRemoteDataSource = authRemoteDataSource,
                sessionStore = sessionStore,
                incidentDao = database.incidentDao(),
                syncMetaDao = database.syncMetaDao(),
                syncScheduler = FakeSyncScheduler(),
            ),
    )

    private fun filledIn(): LoginViewModel = viewModel().apply {
        onUsernameChange(username = "tech1")
        onPasswordChange(password = "password")
    }

    private fun errorAfterLogIn(): LoginError? {
        val viewModel = filledIn()
        viewModel.logIn()
        assertThat(viewModel.uiState.value.isLoggingIn).isFalse()
        return viewModel.uiState.value.error
    }

    private companion object {
        val technicianDto =
            UserDto(
                id = "u1",
                username = "tech1",
                displayName = "Tech One",
                role = "TECHNICIAN",
            )
        val technician =
            User(
                id = "u1",
                username = "tech1",
                displayName = "Tech One",
                role = UserRole.Technician,
            )
    }
}
