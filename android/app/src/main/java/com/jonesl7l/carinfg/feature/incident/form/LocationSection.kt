package com.jonesl7l.carinfg.feature.incident.form

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentLocation
import com.jonesl7l.carinfg.core.domain.validation.FieldError
import com.jonesl7l.carinfg.core.domain.validation.IncidentField

private val touchTargetHeight = 56.dp

@Composable
fun LocationSection(
    fields: IncidentFormFields,
    options: ReferenceOptions,
    errors: List<FieldError>,
    onChange: ((IncidentFormFields) -> IncidentFormFields) -> Unit,
    modifier: Modifier = Modifier,
) {
    val location = fields.location
    val onLocationChange: ((IncidentLocation) -> IncidentLocation) -> Unit = { change -> onChange { it.copy(location = change(it.location)) } }
    val isUnlisted = location.unlistedLocation != null
    val buildingError = errors.reasonFor(field = IncidentField.Building)
    val none = stringResource(id = R.string.option_none)
    FormSection(
        title = R.string.section_location,
        modifier = modifier,
    ) {
        FormDropdown(
            label = R.string.label_site,
            selectedText = options.sites.find { it.id == location.siteId }?.name,
            options = options.sites.map { site ->
                FormOption(
                    label = site.name,
                    onSelect = { onLocationChange { it.copy(siteId = site.id) } },
                )
            },
            error = errors.reasonFor(field = IncidentField.Site),
        )
        FormDropdown(
            label = R.string.label_building,
            selectedText =
                if (isUnlisted) {
                    stringResource(id = R.string.option_location_not_listed)
                } else {
                    options.buildings.find { it.id == location.buildingId }?.name
                },
            options =
                options.buildings.map { building ->
                    FormOption(
                        label = building.name,
                        onSelect = { onLocationChange { it.copy(buildingId = building.id) } },
                    )
                } +
                    FormOption(
                        label = stringResource(id = R.string.option_location_not_listed),
                        onSelect = {
                            onLocationChange {
                                it.copy(
                                    buildingId = null,
                                    unlistedLocation = "",
                                )
                            }
                        },
                    ),
            enabled = location.siteId != null,
            error = buildingError.takeIf { !isUnlisted },
        )
        if (isUnlisted) {
            FormTextField(
                label = R.string.label_unlisted_location,
                value = location.unlistedLocation.orEmpty(),
                onValueChange = { text -> onLocationChange { it.copy(unlistedLocation = text) } },
                error = buildingError,
            )
        } else {
            FormDropdown(
                label = R.string.label_floor,
                selectedText = options.floors.find { it.id == location.floorId }?.name,
                options =
                    listOf(
                        FormOption(
                            label = none,
                            onSelect = { onLocationChange { it.copy(floorId = null) } },
                        ),
                    ) +
                        options.floors.map { floor ->
                            FormOption(
                                label = floor.name,
                                onSelect = { onLocationChange { it.copy(floorId = floor.id) } },
                            )
                        },
                enabled = location.buildingId != null,
            )
            FormDropdown(
                label = R.string.label_room,
                selectedText = options.rooms.find { it.id == location.roomId }?.name,
                options =
                    listOf(
                        FormOption(
                            label = none,
                            onSelect = { onLocationChange { it.copy(roomId = null) } },
                        ),
                    ) +
                        options.rooms.map { room ->
                            FormOption(
                                label = room.name,
                                onSelect = { onLocationChange { it.copy(roomId = room.id) } },
                            )
                        },
                enabled = location.floorId != null,
            )
        }
        FormTextField(
            label = R.string.label_exact_spot,
            value = location.exactSpot.orEmpty(),
            onValueChange = { text -> onLocationChange { it.copy(exactSpot = text.ifEmpty { null }) } },
        )
        FormDropdown(
            label = R.string.label_asset,
            selectedText =
                options.assets.find { it.id == location.assetId }?.let {
                    stringResource(
                        id = R.string.asset_option,
                        it.code,
                        it.name,
                    )
                },
            options =
                listOf(
                    FormOption(
                        label = none,
                        onSelect = { onLocationChange { it.copy(assetId = null) } },
                    ),
                ) +
                    options.assets.map { asset ->
                        FormOption(
                            label =
                                stringResource(
                                    id = R.string.asset_option,
                                    asset.code,
                                    asset.name,
                                ),
                            onSelect = { onLocationChange { it.copy(assetId = asset.id) } },
                        )
                    },
            enabled = location.roomId != null,
        )
        OutOfServiceToggle(
            isChecked = location.equipmentOutOfService,
            isEnabled = location.assetId != null,
            onCheckedChange = { checked -> onLocationChange { it.copy(equipmentOutOfService = checked) } },
        )
    }
}

@Composable
private fun OutOfServiceToggle(
    isChecked: Boolean,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = touchTargetHeight)
                .toggleable(
                    value = isChecked,
                    enabled = isEnabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.label_out_of_service),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(weight = 1f),
        )
        Switch(
            checked = isChecked,
            onCheckedChange = null,
            enabled = isEnabled,
        )
    }
}
