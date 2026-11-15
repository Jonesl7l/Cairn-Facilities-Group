package com.jonesl7l.carinfg.core.sync.push

sealed interface IncidentPushOutcome {
    data object Continue : IncidentPushOutcome

    data object Done : IncidentPushOutcome

    data class Stop(val result: PushResult) : IncidentPushOutcome
}
