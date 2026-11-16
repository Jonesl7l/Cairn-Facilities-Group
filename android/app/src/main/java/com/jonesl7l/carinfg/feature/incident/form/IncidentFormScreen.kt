package com.jonesl7l.carinfg.feature.incident.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonesl7l.carinfg.R

private val sectionSpacing = 10.dp
private val barPadding = 16.dp
private val buttonShape = RoundedCornerShape(14.dp)
private val buttonHeight = 56.dp

@Composable
fun IncidentFormScreen(
    incidentId: String?,
    onClose: () -> Unit,
    viewModel: IncidentFormViewModel =
        hiltViewModel<IncidentFormViewModel, IncidentFormViewModel.Factory>(
            key = incidentId,
            creationCallback = { factory -> factory.create(incidentId = incidentId) },
        ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    IncidentFormContent(
        uiState = uiState,
        onChange = viewModel::update,
        onSubmit = viewModel::submit,
        onClose = onClose,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentFormContent(
    uiState: IncidentFormUiState,
    onChange: ((IncidentFormFields) -> IncidentFormFields) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = if (uiState.isDraft) R.string.form_new_title else R.string.form_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(id = R.string.close),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (!uiState.isLoading) {
                SubmitBar(
                    isDraft = uiState.isDraft,
                    onSubmit = onSubmit,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues = innerPadding),
        ) {
            val fields = uiState.fields
            if (fields == null) {
                CircularProgressIndicator(modifier = Modifier.align(alignment = Alignment.Center))
            } else {
                Column(
                    modifier = Modifier.verticalScroll(state = rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(space = sectionSpacing),
                ) {
                    DetailsSection(
                        fields = fields,
                        errors = uiState.errors,
                        onChange = onChange,
                    )
                    LocationSection(
                        fields = fields,
                        options = uiState.options,
                        errors = uiState.errors,
                        onChange = onChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmitBar(
    isDraft: Boolean,
    onSubmit: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Button(
            onClick = onSubmit,
            shape = buttonShape,
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(all = barPadding)
                    .fillMaxWidth()
                    .height(height = buttonHeight),
        ) {
            Text(text = stringResource(id = if (isDraft) R.string.form_submit else R.string.form_save))
        }
    }
}
