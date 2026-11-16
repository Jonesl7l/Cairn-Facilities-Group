package com.jonesl7l.carinfg.feature.incident.form

import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentDetails
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentType
import com.jonesl7l.carinfg.core.domain.model.incident.Severity
import com.jonesl7l.carinfg.core.domain.validation.FieldError
import com.jonesl7l.carinfg.core.domain.validation.IncidentField
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason
import com.jonesl7l.carinfg.feature.incident.severityLabel
import com.jonesl7l.carinfg.feature.incident.typeLabel
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime

private val severities =
    listOf(
        Severity.Low,
        Severity.Medium,
        Severity.High,
        Severity.Critical,
    )
private val incidentTypes =
    listOf(
        IncidentType.SlipTripFall,
        IncidentType.EquipmentFailure,
        IncidentType.Electrical,
        IncidentType.FireSmoke,
        IncidentType.WaterLeak,
        IncidentType.HazardousSubstance,
        IncidentType.PropertyDamage,
        IncidentType.Security,
        IncidentType.NearMiss,
        IncidentType.Other(description = ""),
    )
private val detailsFields =
    setOf(
        IncidentField.Title,
        IncidentField.Description,
        IncidentField.OccurredAt,
        IncidentField.Type,
        IncidentField.Severity,
    )
private val occurredFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

@Composable
fun DetailsSection(
    fields: IncidentFormFields,
    errors: List<FieldError>,
    remainingErrors: List<FieldError>,
    onChange: ((IncidentFormFields) -> IncidentFormFields) -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = fields.details
    val typeError = errors.reasonFor(field = IncidentField.Type)
    FormSection(
        title = R.string.section_details,
        modifier = modifier,
        errorCount = errors.count { it.field in detailsFields },
        isComplete = remainingErrors.none { it.field in detailsFields },
    ) {
        FormTextField(
            label = R.string.label_title,
            value = details.title,
            onValueChange = { title -> onChange { it.copy(details = it.details.copy(title = title)) } },
            error = errors.reasonFor(field = IncidentField.Title),
        )
        FormChoices(
            label = R.string.label_severity,
            choices = severities,
            selected = details.severity,
            choiceLabel = { severityLabel(severity = it) },
            onSelect = { severity -> onChange { it.copy(details = it.details.copy(severity = severity)) } },
            error = errors.reasonFor(field = IncidentField.Severity),
        )
        FormDropdown(
            label = R.string.label_type,
            selectedText = details.type?.let { optionLabel(type = it) },
            options =
                incidentTypes.map { type ->
                    FormOption(
                        label = optionLabel(type = type),
                        onSelect = { onChange { it.copy(details = it.details.copy(type = type)) } },
                    )
                },
            error = typeError.takeIf { it != ValidationReason.OtherTextRequired },
        )
        val type = details.type
        if (type is IncidentType.Other) {
            FormTextField(
                label = R.string.label_type_other,
                value = type.description,
                onValueChange = { text -> onChange { it.copy(details = it.details.copy(type = IncidentType.Other(description = text))) } },
                error = typeError.takeIf { it == ValidationReason.OtherTextRequired },
            )
        }
        OccurredField(
            details = details,
            error = errors.reasonFor(field = IncidentField.OccurredAt),
            onPick = { occurredAt -> onChange { it.copy(details = it.details.copy(occurredAt = occurredAt)) } },
        )
        FormTextField(
            label = R.string.label_description,
            value = details.description,
            onValueChange = { description -> onChange { it.copy(details = it.details.copy(description = description)) } },
            error = errors.reasonFor(field = IncidentField.Description),
            singleLine = false,
        )
    }
}

@Composable
private fun optionLabel(type: IncidentType): String =
    if (type is IncidentType.Other) stringResource(id = R.string.type_other) else typeLabel(type = type)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OccurredField(
    details: IncidentDetails,
    error: ValidationReason?,
    onPick: (Instant) -> Unit,
) {
    val local = details.occurredAt.toLocalDateTime(timeZone = details.occurredTimeZone)
    var isPickingDate by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    FormDropdown(
        label = R.string.label_occurred,
        selectedText = occurredFormatter.format(local.toJavaLocalDateTime()),
        options = emptyList(),
        error = error,
        onOpen = { isPickingDate = true },
    )
    if (isPickingDate) {
        val dateState =
            rememberDatePickerState(
                initialSelectedDateMillis = local.date.atStartOfDayIn(timeZone = TimeZone.UTC).toEpochMilliseconds(),
            )
        DatePickerDialog(
            onDismissRequest = { isPickingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedDate =
                            dateState.selectedDateMillis?.let {
                                Instant.fromEpochMilliseconds(epochMilliseconds = it).toLocalDateTime(timeZone = TimeZone.UTC).date
                            }
                        isPickingDate = false
                    },
                ) { Text(text = stringResource(id = R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { isPickingDate = false }) { Text(text = stringResource(id = R.string.cancel)) }
            },
        ) {
            DatePicker(state = dateState)
        }
    }
    pickedDate?.let { date ->
        val timeState =
            rememberTimePickerState(
                initialHour = local.hour,
                initialMinute = local.minute,
                is24Hour = DateFormat.is24HourFormat(LocalContext.current),
            )
        AlertDialog(
            onDismissRequest = { pickedDate = null },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPick(
                            LocalDateTime(
                                date = date,
                                time =
                                    LocalTime(
                                        hour = timeState.hour,
                                        minute = timeState.minute,
                                    ),
                            ).toInstant(timeZone = details.occurredTimeZone),
                        )
                        pickedDate = null
                    },
                ) { Text(text = stringResource(id = R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickedDate = null }) { Text(text = stringResource(id = R.string.cancel)) }
            },
        )
    }
}
