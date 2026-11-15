package com.jonesl7l.carinfg.feature.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonesl7l.carinfg.R

private val screenPadding =
    PaddingValues(
        start = 24.dp,
        top = 72.dp,
        end = 24.dp,
        bottom = 32.dp,
    )
private val sectionSpacing = 32.dp
private val fieldSpacing = 16.dp
private val labelSpacing = 6.dp
private val logoBoxSize = 56.dp
private val logoSize = 34.dp
private val logoShape = RoundedCornerShape(16.dp)
private val fieldShape = RoundedCornerShape(12.dp)
private val buttonShape = RoundedCornerShape(14.dp)
private val touchTargetHeight = 56.dp
private val progressSize = 24.dp
private val progressStroke = 2.5.dp
private val companyTextSize = 15.sp
private val labelTextSize = 14.sp

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoginContent(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLogIn = viewModel::logIn,
    )
}

@Composable
fun LoginContent(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surface)
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(state = rememberScrollState())
                .padding(paddingValues = screenPadding),
        verticalArrangement = Arrangement.spacedBy(space = sectionSpacing),
    ) {
        LoginHeader()
        Column(verticalArrangement = Arrangement.spacedBy(space = fieldSpacing)) {
            LabelledField(
                label = R.string.login_username,
                value = uiState.username,
                onValueChange = onUsernameChange,
                readOnly = uiState.isUsernameLocked,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                onImeAction = {},
            )
            LabelledField(
                label = R.string.login_password,
                value = uiState.password,
                onValueChange = onPasswordChange,
                readOnly = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = onLogIn,
            )
            uiState.error?.let { error ->
                Text(
                    text = stringResource(id = error.message()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            LogInButton(
                isLoggingIn = uiState.isLoggingIn,
                enabled = uiState.canLogIn,
                onClick = onLogIn,
            )
        }
    }
}

@Composable
private fun LoginHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(space = fieldSpacing)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(size = logoBoxSize)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = logoShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cairn_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(size = logoSize),
                )
            }
            Text(
                text = stringResource(id = R.string.login_company),
                fontSize = companyTextSize,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = labelSpacing)) {
            Text(
                text = stringResource(id = R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(id = R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LabelledField(
    @StringRes label: Int,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
) {
    val labelText = stringResource(id = label)
    Column(verticalArrangement = Arrangement.spacedBy(space = labelSpacing)) {
        Text(
            text = labelText,
            fontSize = labelTextSize,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = true,
            shape = fieldShape,
            visualTransformation =
                if (keyboardType == KeyboardType.Password) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction,
                ),
            keyboardActions = KeyboardActions { onImeAction() },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = touchTargetHeight),
        )
    }
}

@Composable
private fun LogInButton(
    isLoggingIn: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = buttonShape,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = touchTargetHeight),
    ) {
        if (isLoggingIn) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = progressStroke,
                modifier = Modifier.size(size = progressSize),
            )
        } else {
            Text(
                text = stringResource(id = R.string.log_in),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@StringRes
private fun LoginError.message(): Int = when (this) {
    LoginError.INVALID_CREDENTIALS -> R.string.login_error_invalid_credentials
    LoginError.NOT_TECHNICIAN -> R.string.login_error_not_technician
    LoginError.OFFLINE -> R.string.login_error_offline
    LoginError.FAILED -> R.string.login_error_failed
}
