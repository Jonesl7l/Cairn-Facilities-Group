package com.jonesl7l.carinfg.core.data.session

import com.jonesl7l.carinfg.core.data.mapper.toDomain
import com.jonesl7l.carinfg.core.database.dao.IncidentDao
import com.jonesl7l.carinfg.core.database.dao.SyncMetaDao
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.datasource.AuthRemoteDataSource
import com.jonesl7l.carinfg.core.network.result.ApiError
import com.jonesl7l.carinfg.core.network.result.ApiResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class SessionRepository @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val sessionStore: SessionStore,
    private val incidentDao: IncidentDao,
    private val syncMetaDao: SyncMetaDao,
) {
    fun observeUser(): Flow<User?> = sessionStore.session

    fun observeUnsyncedCount(): Flow<Int> = incidentDao.observeUnsyncedCount()

    suspend fun login(
        username: String,
        password: String,
    ): LoginResult = when (val result = authRemoteDataSource.login(username = username, password = password)) {
        is ApiResult.Failure -> if (result.error == ApiError.InvalidCredentials) {
            LoginResult.InvalidCredentials
        } else {
            LoginResult.Failed(error = result.error)
        }

        is ApiResult.Success -> startSession(user = result.value.toDomain())
    }

    suspend fun logout() {
        // TODO: delete photo folders left by logged-out users after X days without a login
        incidentDao.deleteAll()
        syncMetaDao.reset()
        sessionStore.clear()
    }

    private suspend fun startSession(user: User): LoginResult {
        if (user.role != UserRole.Technician) {
            Timber.i("startSession: rejected %s, not a technician", user.username)
            return LoginResult.NotTechnician
        }
        sessionStore.save(user = user)
        return LoginResult.Success(user = user)
    }
}
