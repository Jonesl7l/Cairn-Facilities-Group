package com.jonesl7l.carinfg.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.app.navigation.IncidentDetailRoute
import com.jonesl7l.carinfg.app.navigation.IncidentFormRoute
import com.jonesl7l.carinfg.app.navigation.IncidentListRoute
import com.jonesl7l.carinfg.app.navigation.LoginRoute
import com.jonesl7l.carinfg.feature.auth.LoginScreen
import com.jonesl7l.carinfg.feature.incident.detail.DeleteOutcome
import com.jonesl7l.carinfg.feature.incident.detail.IncidentDetailScreen
import com.jonesl7l.carinfg.feature.incident.form.FormOutcome
import com.jonesl7l.carinfg.feature.incident.form.IncidentFormScreen
import com.jonesl7l.carinfg.feature.incident.list.IncidentListScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        AppUiState.Loading -> LoadingScreen()

        AppUiState.LoggedOut -> AppBackStack(
            startRoute = LoginRoute,
            isSessionExpired = false,
        )

        is AppUiState.LoggedIn -> AppBackStack(
            startRoute = IncidentListRoute,
            isSessionExpired = state.isSessionExpired,
        )
    }
}

@Composable
private fun AppBackStack(
    startRoute: NavKey,
    isSessionExpired: Boolean,
) {
    val backStack = rememberNavBackStack(startRoute)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val draftSavedMessage = stringResource(R.string.form_draft_saved)
    var pendingUndoId by rememberSaveable { mutableStateOf<String?>(null) }
    SessionExpiredEffect(
        isSessionExpired = isSessionExpired,
        backStack = backStack,
        snackbarHostState = snackbarHostState,
    )
    Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider =
                entryProvider {
                    entry<LoginRoute> { LoginScreen() }
                    entry<IncidentListRoute> {
                        IncidentListScreen(
                            pendingUndoId = pendingUndoId,
                            onUndoFinish = { pendingUndoId = null },
                            onOpenIncident = { incidentId ->
                                backStack.add(IncidentDetailRoute(incidentId = incidentId))
                            },
                            onReportIncident = { backStack.add(IncidentFormRoute(incidentId = null)) },
                        )
                    }
                    entry<IncidentDetailRoute> { route ->
                        IncidentDetailScreen(
                            incidentId = route.incidentId,
                            onBack = { backStack.removeLastOrNull() },
                            onEdit = { backStack.add(IncidentFormRoute(incidentId = route.incidentId)) },
                            onDeleteFinish = { outcome ->
                                if (outcome == DeleteOutcome.UNDOABLE) pendingUndoId = route.incidentId
                                backStack.removeLastOrNull()
                            },
                        )
                    }
                    entry<IncidentFormRoute> { route ->
                        IncidentFormScreen(
                            incidentId = route.incidentId,
                            onFinish = { outcome ->
                                backStack.removeLastOrNull()
                                if (outcome == FormOutcome.DRAFT_SAVED) scope.launch { snackbarHostState.showSnackbar(message = draftSavedMessage) }
                            },
                        )
                    }
                },
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
        )
    }
}

@Composable
private fun SessionExpiredEffect(
    isSessionExpired: Boolean,
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
) {
    val message = stringResource(R.string.session_expired)
    val action = stringResource(R.string.log_in)
    LaunchedEffect(isSessionExpired) {
        if (!isSessionExpired) {
            if (backStack.size > 1 && backStack.last() == LoginRoute) backStack.removeLastOrNull()
            return@LaunchedEffect
        }
        val result =
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = action,
                duration = SnackbarDuration.Indefinite,
            )
        if (result == SnackbarResult.ActionPerformed) backStack.add(LoginRoute)
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
