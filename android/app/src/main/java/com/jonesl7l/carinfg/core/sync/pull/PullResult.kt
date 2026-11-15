package com.jonesl7l.carinfg.core.sync.pull

import com.jonesl7l.carinfg.core.network.result.ApiError

sealed interface PullResult {
    data object Success : PullResult

    data object NoSession : PullResult

    data class Failed(val error: ApiError) : PullResult
}
