package com.jonesl7l.carinfg.core.domain.model.incident

sealed interface ImmediateAction {
    data object None : ImmediateAction

    data object AreaCordonedOff : ImmediateAction

    data object EquipmentIsolated : ImmediateAction

    data object FirstAidGiven : ImmediateAction

    data object EmergencyServicesCalled : ImmediateAction

    data object SupervisorNotified : ImmediateAction

    data object AreaEvacuated : ImmediateAction

    data class Other(val description: String) : ImmediateAction

    data class Unknown(val raw: String) : ImmediateAction
}
