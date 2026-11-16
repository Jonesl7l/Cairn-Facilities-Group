package com.jonesl7l.carinfg.feature.incident.form

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason

private val fieldShape = RoundedCornerShape(12.dp)
private val touchTargetHeight = 56.dp
private val labelSpacing = 6.dp
private const val MULTILINE_MIN_LINES = 3

@Composable
fun FormTextField(
    @StringRes label: Int,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: ValidationReason? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    FieldColumn(
        label = label,
        isError = error != null,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else MULTILINE_MIN_LINES,
            isError = error != null,
            supportingText = error?.let { { Text(text = errorText(reason = it)) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = fieldShape,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = touchTargetHeight),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDropdown(
    @StringRes label: Int,
    selectedText: String?,
    options: List<FormOption>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: ValidationReason? = null,
    onOpen: (() -> Unit)? = null,
) {
    var isExpanded by remember { mutableStateOf(false) }
    FieldColumn(
        label = label,
        isError = error != null,
        modifier = modifier,
    ) {
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { expand ->
                when {
                    !enabled -> Unit
                    onOpen != null -> if (expand) onOpen()
                    else -> isExpanded = expand
                }
            },
        ) {
            OutlinedTextField(
                value = selectedText ?: stringResource(id = R.string.choose),
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(text = errorText(reason = it)) } },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                shape = fieldShape,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = touchTargetHeight)
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option.label) },
                        onClick = {
                            option.onSelect()
                            isExpanded = false
                        },
                        modifier = Modifier.heightIn(min = touchTargetHeight),
                    )
                }
            }
        }
    }
}

@Composable
fun <T> FormChoices(
    @StringRes label: Int,
    choices: List<T>,
    selected: T?,
    choiceLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    error: ValidationReason? = null,
) {
    FieldColumn(
        label = label,
        isError = error != null,
        modifier = modifier,
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            choices.forEachIndexed { index, choice ->
                SegmentedButton(
                    selected = choice == selected,
                    onClick = { onSelect(choice) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = choices.size,
                        ),
                    modifier = Modifier.heightIn(min = touchTargetHeight),
                ) {
                    Text(text = choiceLabel(choice))
                }
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
private fun FieldColumn(
    @StringRes label: Int,
    isError: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = labelSpacing),
    ) {
        Text(
            text = stringResource(id = label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}
