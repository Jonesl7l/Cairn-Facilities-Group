package com.jonesl7l.carinfg.core.domain.validation

sealed interface ValidationReason {
    data object Required : ValidationReason

    data class TooShort(val min: Int) : ValidationReason

    data class TooLong(val max: Int) : ValidationReason

    data class TooMany(val max: Int) : ValidationReason

    data object InFuture : ValidationReason

    data object AfterReportedAt : ValidationReason

    data object OtherTextRequired : ValidationReason

    data object UnknownValue : ValidationReason

    data object AssetRequired : ValidationReason

    data object InjuredPersonRequired : ValidationReason

    data object NoneWithOthers : ValidationReason
}
