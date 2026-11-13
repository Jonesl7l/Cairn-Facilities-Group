package com.jonesl7l.carinfg.core.domain.model.person

sealed interface Treatment {
    data object None : Treatment

    data object FirstAid : Treatment

    data object MedicalTreatment : Treatment

    data object Hospital : Treatment

    data class Unknown(val raw: String) : Treatment
}
