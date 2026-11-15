package com.jonesl7l.carinfg.core.sync

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jonesl7l.carinfg.core.data.session.SessionStore
import com.jonesl7l.carinfg.core.di.ApplicationScope
import com.jonesl7l.carinfg.core.network.events.SyncEvent
import com.jonesl7l.carinfg.core.network.events.SyncEventSource
import com.jonesl7l.carinfg.core.sync.store.SyncLocalStore
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class SseSyncTrigger @Inject constructor(
    private val syncEventSource: SyncEventSource,
    private val syncLocalStore: SyncLocalStore,
    private val syncScheduler: SyncScheduler,
    private val sessionStore: SessionStore,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : DefaultLifecycleObserver {
    private var connection: Job? = null

    override fun onStart(owner: LifecycleOwner) {
        start()
    }

    override fun onStop(owner: LifecycleOwner) {
        stop()
    }

    fun start() {
        if (connection?.isActive == true) return
        connection = applicationScope.launch { followSession() }
    }

    fun stop() {
        connection?.cancel()
        connection = null
    }

    private suspend fun followSession() {
        combine(
            sessionStore.session,
            sessionStore.isSessionExpired,
        ) { user, isExpired -> user != null && !isExpired }
            .distinctUntilChanged()
            .collectLatest { canConnect -> if (canConnect) connectWithRetry() }
    }

    private suspend fun connectWithRetry() {
        while (true) {
            try {
                syncEventSource.events().collect { event -> handle(event = event) }
            } catch (error: IOException) {
                Timber.i(error, "connectWithRetry: event stream dropped, retrying in %s", retryDelay)
            }
            delay(duration = retryDelay)
        }
    }

    private suspend fun handle(event: SyncEvent) {
        when (event) {
            SyncEvent.Connected -> syncScheduler.requestSync()

            is SyncEvent.Hint -> {
                syncLocalStore.raiseAnnouncedChangeSeq(changeSeq = event.changeSeq)
                syncScheduler.requestSync()
            }

            SyncEvent.Unauthorized -> {
                Timber.i("handle: event stream rejected the session")
                sessionStore.markSessionExpired()
            }
        }
    }

    private companion object {
        val retryDelay = 5.seconds
    }
}
