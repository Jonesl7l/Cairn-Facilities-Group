package com.jonesl7l.carinfg.feature.incident.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSafety
import com.jonesl7l.carinfg.core.domain.validation.FieldError
import com.jonesl7l.carinfg.core.domain.validation.IncidentField
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason
import com.jonesl7l.carinfg.feature.incident.actionLabel

private val chipSpacing = 8.dp
private val labelSpacing = 6.dp
private val touchTargetHeight = 56.dp
private val yesNo =
    listOf(
        true,
        false,
    )
private val actions =
    listOf(
        ImmediateAction.None,
        ImmediateAction.AreaCordonedOff,
        ImmediateAction.EquipmentIsolated,
        ImmediateAction.FirstAidGiven,
        ImmediateAction.EmergencyServicesCalled,
        ImmediateAction.SupervisorNotified,
        ImmediateAction.AreaEvacuated,
        ImmediateAction.Other(description = ""),
    )

@Composable
fun SafetySection(
    fields: IncidentFormFields,
    errors: List<FieldError>,
    remainingErrors: List<FieldError>,
    onChange: ((IncidentFormFields) -> IncidentFormFields) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safety = fields.safety
    val onSafetyChange: ((IncidentSafety) -> IncidentSafety) -> Unit = { change -> onChange { it.copy(safety = change(it.safety)) } }
    val actionsError = errors.reasonFor(field = IncidentField.ImmediateActions)
    val other = safety.immediateActions.filterIsInstance<ImmediateAction.Other>().firstOrNull()
    FormSection(
        title = R.string.section_safety,
        modifier = modifier,
        errorCount = errors.count { it.field in safetyFields },
        isComplete = remainingErrors.none { it.field in safetyFields },
    ) {
        FormChoices(
            label = R.string.label_injury_occurred,
            choices = yesNo,
            selected = safety.injuryOccurred,
            choiceLabel = { yesNoLabel(value = it) },
            onSelect = { answer -> onSafetyChange { it.copy(injuryOccurred = answer) } },
            error = errors.reasonFor(field = IncidentField.InjuryOccurred),
        )
        FormChoices(
            label = R.string.label_hazard_present,
            choices = yesNo,
            selected = safety.hazardStillPresent,
            choiceLabel = { yesNoLabel(value = it) },
            onSelect = { answer -> onSafetyChange { it.copy(hazardStillPresent = answer) } },
            error = errors.reasonFor(field = IncidentField.HazardStillPresent),
        )
        ActionChips(
            selected = safety.immediateActions,
            error = actionsError.takeIf { it != ValidationReason.OtherTextRequired },
            onToggle = { action ->
                onSafetyChange {
                    it.copy(
                        immediateActions = toggle(
                            actions = it.immediateActions,
                            action = action,
                        ),
                    )
                }
            },
        )
        if (other != null) {
            FormTextField(
                label = R.string.label_action_other,
                value = other.description,
                onValueChange = { text ->
                    onSafetyChange {
                        it.copy(
                            immediateActions =
                                it.immediateActions.filterNot { action -> action is ImmediateAction.Other }.toSet() +
                                    ImmediateAction.Other(description = text),
                        )
                    }
                },
                error = actionsError.takeIf { it == ValidationReason.OtherTextRequired },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionChips(
    selected: Set<ImmediateAction>,
    error: ValidationReason?,
    onToggle: (ImmediateAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = labelSpacing)) {
        Text(
            text = stringResource(id = R.string.label_immediate_actions),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(space = chipSpacing)) {
            actions.forEach { action ->
                FilterChip(
                    selected = isSelected(
                        selected = selected,
                        action = action,
                    ),
                    onClick = { onToggle(action) },
                    label = { Text(text = actionLabel(action = action)) },
                    modifier = Modifier.heightIn(min = touchTargetHeight),
                )
            }
        }
        error?.let {
            Text(
                text = errorText(reason = it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun yesNoLabel(value: Boolean): String = stringResource(id = if (value) R.string.value_yes else R.string.value_no)

private fun isSelected(
    selected: Set<ImmediateAction>,
    action: ImmediateAction,
): Boolean = if (action is ImmediateAction.Other) selected.any { it is ImmediateAction.Other } else action in selected

private fun toggle(
    actions: Set<ImmediateAction>,
    action: ImmediateAction,
): Set<ImmediateAction> = when {
    action is ImmediateAction.Other -> {
        if (actions.any { it is ImmediateAction.Other }) actions.filterNot { it is ImmediateAction.Other }.toSet() else actions + action
    }

    action in actions -> actions - action

    else -> actions + action
}
