package com.jonesl7l.carinfg.feature.incident.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jonesl7l.carinfg.R

@Composable
fun NotesSection(
    fields: IncidentFormFields,
    onChange: ((IncidentFormFields) -> IncidentFormFields) -> Unit,
    modifier: Modifier = Modifier,
) {
    FormSection(
        title = R.string.section_notes,
        modifier = modifier,
    ) {
        FormTextField(
            label = R.string.label_additional_notes,
            value = fields.additionalNotes.orEmpty(),
            onValueChange = { text -> onChange { it.copy(additionalNotes = text.ifEmpty { null }) } },
            singleLine = false,
        )
    }
}
