package com.jonesl7l.carinfg.feature.incident.form

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.ui.theme.LocalStatusColors

private val sectionPadding = 20.dp
private val sectionVerticalPadding = 16.dp
private val itemSpacing = 14.dp
private val statusSpacing = 4.dp
private val statusIconSize = 16.dp

@Composable
fun FormSection(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    errorCount: Int = 0,
    isComplete: Boolean = false,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = sectionPadding,
                        vertical = sectionVerticalPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(space = itemSpacing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(weight = 1f),
                )
                when {
                    errorCount > 0 -> SectionStatus(
                        icon = R.drawable.ic_warning,
                        text = stringResource(id = R.string.section_to_fix, errorCount),
                        color = MaterialTheme.colorScheme.error,
                    )

                    isComplete -> SectionStatus(
                        icon = R.drawable.ic_check,
                        text = stringResource(id = R.string.section_complete),
                        color = LocalStatusColors.current.upToDate,
                    )

                    hint != null -> Text(
                        text = hint,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun SectionStatus(
    @DrawableRes icon: Int,
    text: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = statusSpacing),
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size = statusIconSize),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
