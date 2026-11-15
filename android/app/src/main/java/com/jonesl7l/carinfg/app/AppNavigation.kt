package com.jonesl7l.carinfg.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
                    entry<IncidentListRoute> { PlaceholderScreen(route = it) }
                    entry<IncidentDetailRoute> { PlaceholderScreen(route = it) }
                    entry<IncidentFormRoute> { PlaceholderScreen(route = it) }
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

@Composable
private fun PlaceholderScreen(route: NavKey) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = route.toString())
    }
}
