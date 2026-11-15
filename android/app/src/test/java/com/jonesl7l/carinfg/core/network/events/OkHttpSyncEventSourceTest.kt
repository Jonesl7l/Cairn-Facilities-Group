package com.jonesl7l.carinfg.core.network.events

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.jonesl7l.carinfg.core.network.http.HttpHeaders
import com.jonesl7l.carinfg.core.network.http.HttpStatus
import com.jonesl7l.carinfg.core.network.http.MediaTypes
import java.io.IOException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test

class OkHttpSyncEventSourceTest {
    private val server = MockWebServer()
    private lateinit var eventSource: OkHttpSyncEventSource

    @Before
    fun setUp() {
        server.start()
        eventSource = OkHttpSyncEventSource(
            client = OkHttpClient(),
            eventsUrl = server.url("/events"),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `hints arrive after the connection opens and the flow ends when the stream closes`() = runTest {
        server.enqueue(
            stream(body = "event: incidents-changed\nid: 5\ndata: 5\n\nevent: incidents-changed\nid: 6\ndata: 6\n\n"),
        )

        val events = eventSource.events().toList()

        assertThat(
            events,
        ).containsExactly(SyncEvent.Connected, SyncEvent.Hint(changeSeq = 5), SyncEvent.Hint(changeSeq = 6))
        assertThat(server.takeRequest().headers[HttpHeaders.ACCEPT]).isEqualTo(MediaTypes.EVENT_STREAM)
    }

    @Test
    fun `heartbeats, other events and bad data are ignored`() = runTest {
        server.enqueue(
            stream(body = ": heartbeat\n\nevent: other\ndata: 1\n\nevent: incidents-changed\ndata: oops\n\n"),
        )

        assertThat(eventSource.events().toList()).containsExactly(SyncEvent.Connected)
    }

    @Test
    fun `a 401 is unauthorized and ends the flow`() = runTest {
        server.enqueue(MockResponse.Builder().code(HttpStatus.UNAUTHORIZED).build())

        assertThat(eventSource.events().toList()).containsExactly(SyncEvent.Unauthorized)
    }

    @Test
    fun `any other failure ends the flow with an IOException`() = runTest {
        server.enqueue(MockResponse.Builder().code(HttpStatus.INTERNAL_SERVER_ERROR).build())

        assertFailure { eventSource.events().toList() }.isInstanceOf<IOException>()
    }

    private fun stream(body: String): MockResponse = MockResponse.Builder()
        .addHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.EVENT_STREAM)
        .body(body)
        .build()
}
