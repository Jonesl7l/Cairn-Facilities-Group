package com.jonesl7l.carinfg.core.domain.model.incident

data class IncidentSafety(
    val injuryOccurred: Boolean? = null,
    val hazardStillPresent: Boolean? = null,
    val immediateActions: Set<ImmediateAction> = emptySet(),
)
