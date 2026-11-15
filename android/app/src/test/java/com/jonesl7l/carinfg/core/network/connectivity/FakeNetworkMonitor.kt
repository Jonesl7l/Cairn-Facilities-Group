package com.jonesl7l.carinfg.core.network.connectivity

import kotlinx.coroutines.flow.MutableStateFlow

class FakeNetworkMonitor(initial: Boolean = true) : NetworkMonitor {
    override val isOnline = MutableStateFlow(initial)
}
