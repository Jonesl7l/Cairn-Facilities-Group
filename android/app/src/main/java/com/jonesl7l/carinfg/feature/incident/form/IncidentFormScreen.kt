package com.jonesl7l.carinfg.feature.incident.form

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.validation.FieldError
import com.jonesl7l.carinfg.core.domain.validation.IncidentField
import kotlin.math.roundToInt

private val sectionSpacing = 10.dp
private val barPadding = 16.dp
private val buttonShape = RoundedCornerShape(14.dp)
private val buttonHeight = 56.dp
private const val DETAILS_INDEX = 0
private const val LOCATION_INDEX = 1
private const val SAFETY_INDEX = 2
private const val PEOPLE_INDEX = 3
private val sectionMatchers: List<(IncidentField) -> Boolean> =
    listOf(
        { it in detailsFields },
        { it in locationFields },
        { it in safetyFields },
        { it is IncidentField.Person },
    )

@Composable
fun IncidentFormScreen(
    incidentId: String?,
    onFinish: (FormOutcome) -> Unit,
    viewModel: IncidentFormViewModel =
        hiltViewModel<IncidentFormViewModel, IncidentFormViewModel.Factory>(
            key = incidentId,
            creationCallback = { factory -> factory.create(incidentId = incidentId) },
        ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(event = Lifecycle.Event.ON_STOP) { viewModel.saveDraft() }
    IncidentFormContent(
        uiState = uiState,
        onChange = viewModel::update,
        onAddPerson = viewModel::addPerson,
        onRemovePerson = viewModel::removePerson,
        onCameraTarget = viewModel::cameraTarget,
        onPhotoTake = viewModel::onPhotoTaken,
        onPick = viewModel::addPicked,
        onMakeCover = viewModel::makeCover,
        onRemovePhoto = viewModel::removePhoto,
        onSubmit = viewModel::submit,
        onCloseDraft = viewModel::closeDraft,
        onOutcomeConsume = viewModel::consumeOutcome,
        onFinish = onFinish,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentFormContent(
    uiState: IncidentFormUiState,
    onChange: ((IncidentFormFields) -> IncidentFormFields) -> Unit,
    onAddPerson: () -> Unit,
    onRemovePerson: (Int) -> Unit,
    onCameraTarget: () -> Uri,
    onPhotoTake: (Boolean) -> Unit,
    onPick: (List<Uri>) -> Unit,
    onMakeCover: (Int) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onSubmit: () -> Unit,
    onCloseDraft: () -> Unit,
    onOutcomeConsume: () -> Unit,
    onFinish: (FormOutcome) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val sectionOffsets = remember { mutableStateMapOf<Int, Int>() }
    var isDiscarding by rememberSaveable { mutableStateOf(false) }
    val notAllowedMessage = stringResource(id = R.string.form_not_allowed)
    val photoNotSavedMessage = stringResource(id = R.string.form_photo_not_saved)
    val currentOnFinish by rememberUpdatedState(onFinish)
    val currentOnOutcomeConsume by rememberUpdatedState(onOutcomeConsume)
    val close = {
        when {
            uiState.isDraft -> onCloseDraft()
            uiState.hasChanges -> isDiscarding = true
            else -> onFinish(FormOutcome.CLOSED)
        }
    }
    BackHandler(onBack = close)
    LaunchedEffect(uiState.outcome) {
        val outcome = uiState.outcome ?: return@LaunchedEffect
        when (outcome) {
            FormOutcome.INVALID -> {
                val offset = firstErrorSection(errors = uiState.errors)?.let { sectionOffsets[it] }
                if (offset != null) scrollState.animateScrollTo(value = offset)
                currentOnOutcomeConsume()
            }

            FormOutcome.NOT_ALLOWED -> {
                snackbarHostState.showSnackbar(message = notAllowedMessage)
                currentOnOutcomeConsume()
            }

            FormOutcome.PHOTO_NOT_SAVED -> {
                snackbarHostState.showSnackbar(message = photoNotSavedMessage)
                currentOnOutcomeConsume()
            }

            else -> currentOnFinish(outcome)
        }
    }
    if (isDiscarding) {
        DiscardDialog(
            onDiscard = {
                isDiscarding = false
                onFinish(FormOutcome.CLOSED)
            },
            onKeepEditing = { isDiscarding = false },
        )
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = if (uiState.isDraft) R.string.form_new_title else R.string.form_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = close) {
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
                    modifier = Modifier.verticalScroll(state = scrollState),
                    verticalArrangement = Arrangement.spacedBy(space = sectionSpacing),
                ) {
                    PhotosSection(
                        photos = fields.photos,
                        photosLeft = uiState.photosLeft,
                        onCameraTarget = onCameraTarget,
                        onPhotoTake = onPhotoTake,
                        onPick = onPick,
                        onMakeCover = onMakeCover,
                        onRemove = onRemovePhoto,
                    )
                    DetailsSection(
                        fields = fields,
                        errors = uiState.errors,
                        remainingErrors = uiState.remainingErrors,
                        onChange = onChange,
                        modifier = Modifier.recordOffset(
                            index = DETAILS_INDEX,
                            offsets = sectionOffsets,
                        ),
                    )
                    LocationSection(
                        fields = fields,
                        options = uiState.options,
                        errors = uiState.errors,
                        remainingErrors = uiState.remainingErrors,
                        onChange = onChange,
                        modifier = Modifier.recordOffset(
                            index = LOCATION_INDEX,
                            offsets = sectionOffsets,
                        ),
                    )
                    SafetySection(
                        fields = fields,
                        errors = uiState.errors,
                        remainingErrors = uiState.remainingErrors,
                        onChange = onChange,
                        modifier = Modifier.recordOffset(
                            index = SAFETY_INDEX,
                            offsets = sectionOffsets,
                        ),
                    )
                    PeopleSection(
                        fields = fields,
                        errors = uiState.errors,
                        remainingErrors = uiState.remainingErrors,
                        canAddPerson = uiState.canAddPerson,
                        onChange = onChange,
                        onAddPerson = onAddPerson,
                        onRemovePerson = onRemovePerson,
                        modifier = Modifier.recordOffset(
                            index = PEOPLE_INDEX,
                            offsets = sectionOffsets,
                        ),
                    )
                    NotesSection(
                        fields = fields,
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

@Composable
private fun DiscardDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(text = stringResource(id = R.string.discard_title)) },
        text = { Text(text = stringResource(id = R.string.discard_body)) },
        confirmButton = {
            TextButton(onClick = onDiscard) { Text(text = stringResource(id = R.string.discard)) }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) { Text(text = stringResource(id = R.string.keep_editing)) }
        },
    )
}

private fun firstErrorSection(errors: List<FieldError>): Int? =
    sectionMatchers.indexOfFirst { matches -> errors.any { matches(it.field) } }.takeIf { it >= 0 }

private fun Modifier.recordOffset(
    index: Int,
    offsets: MutableMap<Int, Int>,
): Modifier = onGloballyPositioned { offsets[index] = it.positionInParent().y.roundToInt() }
