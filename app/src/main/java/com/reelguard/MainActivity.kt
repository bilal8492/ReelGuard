package com.reelguard

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reelguard.data.datastore.Settings
import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.data.datastore.ThemeMode
import com.reelguard.ui.common.FloatingNavBar
import com.reelguard.ui.common.NavItem
import com.reelguard.ui.common.NavKind
import com.reelguard.ui.dashboard.DashboardScreen
import com.reelguard.ui.debug.DebugScreen
import com.reelguard.ui.health.HealthScreen
import com.reelguard.ui.history.HistoryScreen
import com.reelguard.ui.onboarding.OnboardingScreen
import com.reelguard.ui.settings.SettingsScreen
import com.reelguard.ui.theme.ReelGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(settings: SettingsRepository) : ViewModel() {
        val settings: StateFlow<Settings?> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }

object Routes {
    const val ONBOARDING = "onboarding"
    const val TODAY = "today"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val HEALTH = "health"
    const val DEBUG = "debug"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            val vm: MainViewModel = hiltViewModel()
            val s by vm.settings.collectAsStateWithLifecycle()
            val mode = s?.theme ?: ThemeMode.SYSTEM
            val dark =
                when (mode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
            // System bar icons follow the app theme (which the user can set differently from the phone's).
            DisposableEffect(dark) {
                val style =
                    if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            ReelGuardTheme(mode) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    s?.let { AppNav(startOnboarding = !it.onboardingDone) }
                }
            }
        }
    }
}

@Composable
fun AppNav(startOnboarding: Boolean) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination
    val items =
        listOf(
            NavItem(Routes.TODAY, stringResource(R.string.nav_today), NavKind.TODAY),
            NavItem(Routes.HISTORY, stringResource(R.string.nav_history), NavKind.HISTORY),
            NavItem(Routes.SETTINGS, stringResource(R.string.nav_settings), NavKind.SETTINGS),
        )
    val selected = items.firstOrNull { item -> current?.hierarchy?.any { it.route == item.route } == true }?.route
    Box(Modifier.fillMaxSize()) {
        NavHost(
            nav,
            startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.TODAY,
            modifier = Modifier.statusBarsPadding(),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onDone = { nav.navigate(Routes.TODAY) { popUpTo(Routes.ONBOARDING) { inclusive = true } } })
            }
            composable(Routes.TODAY) { DashboardScreen(onOpenHealth = { nav.navigate(Routes.HEALTH) }) }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(onOpenHealth = { nav.navigate(Routes.HEALTH) }, onOpenDebug = { nav.navigate(Routes.DEBUG) })
            }
            composable(Routes.HEALTH) { HealthScreen() }
            composable(Routes.DEBUG) { DebugScreen() }
        }
        if (selected != null) {
            FloatingNavBar(
                items = items,
                selectedRoute = selected,
                onSelect = { route ->
                    nav.navigate(route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp),
            )
        }
    }
}
