package com.jonesl7l.carinfg.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class IncidentDetailRoute(val incidentId: String) : NavKey
