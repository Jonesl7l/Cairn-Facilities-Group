package com.jonesl7l.carinfg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jonesl7l.carinfg.app.AppNavigation
import com.jonesl7l.carinfg.app.AppUiState
import com.jonesl7l.carinfg.app.AppViewModel
import com.jonesl7l.carinfg.core.ui.theme.CarinfgTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        val splashDeadline = TimeSource.Monotonic.markNow() + maxSplashDuration
        splashScreen.setKeepOnScreenCondition {
            appViewModel.uiState.value == AppUiState.Loading && splashDeadline.hasNotPassedNow()
        }
        enableEdgeToEdge()
        setContent {
            CarinfgTheme {
                AppNavigation(viewModel = appViewModel)
            }
        }
    }

    private companion object {
        val maxSplashDuration = 5.seconds
    }
}
