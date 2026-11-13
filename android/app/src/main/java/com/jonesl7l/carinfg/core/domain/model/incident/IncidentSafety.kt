package com.jonesl7l.carinfg.core.domain.model.incident

data class IncidentSafety(
    val injuryOccurred: Boolean?,
    val hazardStillPresent: Boolean?,
    val immediateActions: Set<ImmediateAction>,
)
