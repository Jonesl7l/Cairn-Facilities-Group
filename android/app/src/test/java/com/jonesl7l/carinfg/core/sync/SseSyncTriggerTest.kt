package com.jonesl7l.carinfg.core.sync

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.jonesl7l.carinfg.core.data.session.FakeSessionStore
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.domain.model.user.UserRole
import com.jonesl7l.carinfg.core.network.events.FakeSyncEventSource
import com.jonesl7l.carinfg.core.network.events.SyncEvent
import com.jonesl7l.carinfg.core.sync.store.FakeSyncLocalStore
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SseSyncTriggerTest {
    private val eventSource = FakeSyncEventSource()
    private val store = FakeSyncLocalStore()
    private val scheduler = FakeSyncScheduler()
    private val sessionStore = FakeSessionStore(initial = technician)

    @Test
    fun `coming to the foreground connects and asks for a sync`() = runTest {
        trigger().start()
        runCurrent()

        assertThat(eventSource.connections).isEqualTo(1)
        assertThat(scheduler.syncRequests).isEqualTo(1)
    }

    @Test
    fun `nothing connects without a session`() = runTest {
        sessionStore.clear()

        trigger().start()
        runCurrent()

        assertThat(eventSource.connections).isEqualTo(0)
        assertThat(scheduler.syncRequests).isEqualTo(0)
    }

    @Test
    fun `each hint raises the announced change and asks for a sync`() = runTest {
        eventSource.streams +=
            flow {
                emit(SyncEvent.Connected)
                emit(SyncEvent.Hint(changeSeq = 5))
                emit(SyncEvent.Hint(changeSeq = 7))
                awaitCancellation()
            }
        trigger().start()
        runCurrent()

        assertThat(scheduler.syncRequests).isEqualTo(3)
        assertThat(store.meta.announcedChangeSeq).isEqualTo(7L)
    }

    @Test
    fun `a dropped stream reconnects after five seconds`() = runTest {
        eventSource.streams += flow { throw IOException("dropped") }
        trigger().start()
        runCurrent()

        advanceTimeBy(delayTime = 4.seconds)
        assertThat(eventSource.connections).isEqualTo(1)
        advanceTimeBy(delayTime = 1.seconds + 1.milliseconds)

        assertThat(eventSource.connections).isEqualTo(2)
    }

    @Test
    fun `a stream the server closes reconnects after five seconds`() = runTest {
        eventSource.streams += flowOf(SyncEvent.Connected)
        trigger().start()
        runCurrent()

        advanceTimeBy(delayTime = 5.seconds + 1.milliseconds)

        assertThat(eventSource.connections).isEqualTo(2)
    }

    @Test
    fun `an unauthorized stream marks the session expired and stops reconnecting`() = runTest {
        eventSource.streams += flowOf(SyncEvent.Unauthorized)
        trigger().start()
        runCurrent()

        advanceTimeBy(delayTime = 30.seconds)

        assertThat(sessionStore.isSessionExpired.first()).isTrue()
        assertThat(eventSource.connections).isEqualTo(1)
    }

    @Test
    fun `going to the background closes the stream`() = runTest {
        val trigger = trigger()
        trigger.start()
        runCurrent()

        trigger.stop()
        runCurrent()

        assertThat(eventSource.openStreams).isEqualTo(0)
    }

    @Test
    fun `logging out closes the stream`() = runTest {
        trigger().start()
        runCurrent()

        sessionStore.clear()
        runCurrent()

        assertThat(eventSource.openStreams).isEqualTo(0)
    }

    private fun TestScope.trigger() = SseSyncTrigger(
        syncEventSource = eventSource,
        syncLocalStore = store,
        syncScheduler = scheduler,
        sessionStore = sessionStore,
        applicationScope = backgroundScope,
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
