package com.jonesl7l.carinfg.feature.incident.form

import com.jonesl7l.carinfg.core.domain.validation.FieldError

data class IncidentFormUiState(
    val fields: IncidentFormFields? = null,
    val isDraft: Boolean = true,
    val options: ReferenceOptions = ReferenceOptions(),
    val errors: List<FieldError> = emptyList(),
    val hasChanges: Boolean = false,
    val canAddPerson: Boolean = true,
    val outcome: FormOutcome? = null,
) {
    val isLoading: Boolean get() = fields == null
}
