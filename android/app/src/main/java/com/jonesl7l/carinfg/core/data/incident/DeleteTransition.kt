package com.jonesl7l.carinfg.core.data.incident

import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity

sealed interface DeleteTransition {
    data object Remove : DeleteTransition

    data class MarkPending(val entity: IncidentEntity) : DeleteTransition

    data object NotAllowed : DeleteTransition
}
