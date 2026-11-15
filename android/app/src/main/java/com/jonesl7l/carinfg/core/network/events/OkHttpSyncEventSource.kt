package com.jonesl7l.carinfg.core.network.events

import com.jonesl7l.carinfg.core.network.http.HttpHeaders
import com.jonesl7l.carinfg.core.network.http.HttpStatus
import com.jonesl7l.carinfg.core.network.http.MediaTypes
import java.io.IOException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import timber.log.Timber

class OkHttpSyncEventSource(
    client: OkHttpClient,
    private val eventsUrl: HttpUrl,
) : SyncEventSource {
    private val factory = EventSources.createFactory(callFactory = client)

    override fun events(): Flow<SyncEvent> = callbackFlow {
        val request =
            Request.Builder()
                .url(eventsUrl)
                .header(HttpHeaders.ACCEPT, MediaTypes.EVENT_STREAM)
                .build()
        val listener =
            object : EventSourceListener() {
                override fun onOpen(
                    eventSource: EventSource,
                    response: Response,
                ) {
                    trySend(SyncEvent.Connected)
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String,
                ) {
                    if (type != INCIDENTS_CHANGED) return
                    val changeSeq = data.toLongOrNull()
                    if (changeSeq == null) {
                        Timber.w("onEvent: ignored a hint with data %s", data)
                        return
                    }
                    trySend(SyncEvent.Hint(changeSeq = changeSeq))
                }

                override fun onClosed(eventSource: EventSource) {
                    channel.close()
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?,
                ) {
                    if (response?.code == HttpStatus.UNAUTHORIZED) {
                        trySend(SyncEvent.Unauthorized)
                        channel.close()
                        return
                    }
                    channel.close(cause = t as? IOException ?: IOException("onFailure: stream failed", t))
                }
            }
        val eventSource = factory.newEventSource(request = request, listener = listener)
        awaitClose { eventSource.cancel() }
    }

    private companion object {
        const val INCIDENTS_CHANGED = "incidents-changed"
    }
}
