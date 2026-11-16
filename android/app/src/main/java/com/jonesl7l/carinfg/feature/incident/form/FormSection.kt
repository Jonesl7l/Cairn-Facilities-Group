package com.jonesl7l.carinfg.feature.incident.form

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

private val sectionPadding = 20.dp
private val sectionVerticalPadding = 16.dp
private val itemSpacing = 14.dp

@Composable
fun FormSection(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
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
            Text(
                text = stringResource(id = title),
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}
