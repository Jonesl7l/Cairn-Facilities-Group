package com.jonesl7l.carinfg.feature.incident.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.validation.FieldError
import com.jonesl7l.carinfg.core.domain.validation.IncidentField
import com.jonesl7l.carinfg.core.domain.validation.ValidationReason

fun List<FieldError>.reasonFor(field: IncidentField): ValidationReason? = firstOrNull { it.field == field }?.reason

@Composable
fun errorText(reason: ValidationReason): String = when (reason) {
    ValidationReason.Required -> stringResource(id = R.string.error_required)
    is ValidationReason.TooShort -> stringResource(id = R.string.error_too_short, reason.min)
    is ValidationReason.TooLong -> stringResource(id = R.string.error_too_long, reason.max)
    is ValidationReason.TooMany -> stringResource(id = R.string.error_too_many, reason.max)
    ValidationReason.InFuture -> stringResource(id = R.string.error_in_future)
    ValidationReason.AfterReportedAt -> stringResource(id = R.string.error_after_reported)
    ValidationReason.OtherTextRequired -> stringResource(id = R.string.error_other_text)
    ValidationReason.UnknownValue -> stringResource(id = R.string.error_unknown_value)
    ValidationReason.AssetRequired -> stringResource(id = R.string.error_asset_required)
    ValidationReason.InjuredPersonRequired -> stringResource(id = R.string.error_injured_person_required)
    ValidationReason.NoneWithOthers -> stringResource(id = R.string.error_none_with_others)
}
