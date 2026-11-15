package com.jonesl7l.carinfg.feature.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.ui.theme.LocalStatusColors

private val sheetPadding = 20.dp
private val sectionSpacing = 20.dp
private val rowSpacing = 12.dp
private val avatarSize = 52.dp
private val boxPadding = 14.dp
private val boxShape = RoundedCornerShape(14.dp)
private val buttonShape = RoundedCornerShape(14.dp)
private val buttonHeight = 52.dp
private val borderWidth = 1.5.dp
private val iconSize = 20.dp
private val progressSize = 20.dp
private val progressStroke = 2.dp
private val avatarTextSize = 18.sp
private val nameTextSize = 18.sp
private val detailTextSize = 14.sp
private val bodyTextSize = 13.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(
    onDismiss: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        AccountContent(
            uiState = uiState,
            onLogOut = viewModel::logOut,
        )
    }
}

@Composable
fun AccountContent(
    uiState: AccountUiState,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = sheetPadding,
                    end = sheetPadding,
                    bottom = sheetPadding,
                ),
        verticalArrangement = Arrangement.spacedBy(space = sectionSpacing),
    ) {
        UserHeader(uiState = uiState)
        if (uiState.unsyncedCount > 0) UnsyncedBox(count = uiState.unsyncedCount)
        LogOutButton(
            unsyncedCount = uiState.unsyncedCount,
            isLoggingOut = uiState.isLoggingOut,
            onClick = onLogOut,
        )
    }
}

@Composable
private fun UserHeader(uiState: AccountUiState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = rowSpacing),
    ) {
        Box(
            modifier =
                Modifier
                    .size(size = avatarSize)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = uiState.initials,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = avatarTextSize,
                fontWeight = FontWeight.Bold,
            )
        }
        Column {
            Text(
                text = uiState.displayName,
                fontSize = nameTextSize,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(id = R.string.account_role, uiState.username),
                fontSize = detailTextSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UnsyncedBox(count: Int) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = boxShape,
                ).padding(all = boxPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = rowSpacing),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_cloud_upload),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column {
            Text(
                text =
                    pluralStringResource(
                        id = R.plurals.account_unsynced_title,
                        count = count,
                        count,
                    ),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(id = R.string.account_unsynced_body),
                fontSize = bodyTextSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LogOutButton(
    unsyncedCount: Int,
    isLoggingOut: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalStatusColors.current
    Column(verticalArrangement = Arrangement.spacedBy(space = rowSpacing)) {
        if (unsyncedCount > 0) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = colors.attentionContainer,
                            shape = boxShape,
                        ).padding(all = boxPadding),
                horizontalArrangement = Arrangement.spacedBy(space = rowSpacing),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_warning),
                    contentDescription = null,
                    tint = colors.onAttention,
                    modifier = Modifier.size(size = iconSize),
                )
                Text(
                    text =
                        pluralStringResource(
                            id = R.plurals.account_logout_warning,
                            count = unsyncedCount,
                            unsyncedCount,
                        ),
                    fontSize = detailTextSize,
                )
            }
        }
        OutlinedButton(
            onClick = onClick,
            enabled = !isLoggingOut,
            shape = buttonShape,
            border =
                BorderStroke(
                    width = borderWidth,
                    color = MaterialTheme.colorScheme.error,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = buttonHeight),
        ) {
            if (isLoggingOut) {
                CircularProgressIndicator(
                    strokeWidth = progressStroke,
                    modifier = Modifier.size(size = progressSize),
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logout),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(size = iconSize),
                )
                Text(
                    text = stringResource(id = if (unsyncedCount > 0) R.string.log_out_anyway else R.string.log_out),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = rowSpacing),
                )
            }
        }
    }
}
