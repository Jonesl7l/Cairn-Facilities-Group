package com.jonesl7l.carinfg.feature.incident

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jonesl7l.carinfg.R

private val badgePadding = 6.dp
private val badgeVerticalPadding = 2.dp
private val badgeShape = RoundedCornerShape(size = 6.dp)

@Composable
fun CoverBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = R.string.photo_cover),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.inverseOnSurface,
        modifier =
            modifier
                .padding(all = badgePadding)
                .background(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shape = badgeShape,
                )
                .padding(
                    horizontal = badgePadding,
                    vertical = badgeVerticalPadding,
                ),
    )
}
