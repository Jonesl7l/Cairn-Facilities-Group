package com.jonesl7l.carinfg.core.sync.push

import com.jonesl7l.carinfg.core.sync.SyncRunResult

sealed interface IncidentPushOutcome {
    data object Continue : IncidentPushOutcome

    data object Done : IncidentPushOutcome

    data class Stop(val result: SyncRunResult) : IncidentPushOutcome
}
