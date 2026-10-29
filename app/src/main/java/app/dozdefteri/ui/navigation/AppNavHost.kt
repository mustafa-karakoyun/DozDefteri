package app.dozdefteri.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material.icons.filled.Today as TodayIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.dozdefteri.MainActivity
import app.dozdefteri.R
import app.dozdefteri.ui.battery.BatteryGuideScreen
import app.dozdefteri.ui.export.ExportScreen
import app.dozdefteri.ui.history.HistoryScreen
import app.dozdefteri.ui.medications.MedicationEditScreen
import app.dozdefteri.ui.medications.MedicationsScreen
import app.dozdefteri.ui.onboarding.OnboardingScreen
import app.dozdefteri.ui.pro.ProScreen
import app.dozdefteri.ui.profiles.ProfilesScreen
import app.dozdefteri.ui.settings.SettingsScreen
import app.dozdefteri.ui.today.TodayScreen
import kotlinx.coroutines.launch

private data class TopLevelTab(val route: Any, @StringRes val label: Int, val icon: ImageVector)

private val topLevelTabs = listOf(
    TopLevelTab(Today, R.string.tab_today, Icons.Filled.TodayIcon),
    TopLevelTab(Medications, R.string.tab_medications, Icons.AutoMirrored.Filled.List),
    TopLevelTab(History, R.string.tab_history, Icons.Filled.CalendarMonth),
    TopLevelTab(Settings, R.string.tab_settings, Icons.Filled.SettingsIcon)
)

@Composable
fun AppNavHost(
    onboardingDone: Boolean,
    pendingDestination: String?,
    onDestinationHandled: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = topLevelTabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Fixed for the lifetime of the host: changing it later would reset the back stack.
    val startDestination: Any = remember { if (onboardingDone) Today else Onboarding }
    val savedMessage = stringResource(R.string.saved)
    val showSaved: () -> Unit = { scope.launch { snackbarHostState.showSnackbar(savedMessage) } }

    LaunchedEffect(pendingDestination, onboardingDone) {
        if (pendingDestination == null || !onboardingDone) return@LaunchedEffect
        when (pendingDestination) {
            MainActivity.DESTINATION_TODAY -> navController.navigateToTab(Today)
            MainActivity.DESTINATION_EXPORT -> navController.navigate(Export)
            MainActivity.DESTINATION_PRO -> navController.navigate(Pro)
        }
        onDestinationHandled()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelTabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()).consumeWindowInsets(padding)
        ) {
            composable<Onboarding> {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Today) {
                            popUpTo<Onboarding> { inclusive = true }
                        }
                    },
                    onOpenBatteryGuide = { navController.navigate(BatteryGuide) }
                )
            }
            composable<Today> {
                TodayScreen(onAddMedication = { navController.navigate(MedicationEdit()) })
            }
            composable<Medications> {
                MedicationsScreen(
                    onAdd = { navController.navigate(MedicationEdit()) },
                    onOpen = { id -> navController.navigate(MedicationEdit(id)) }
                )
            }
            composable<MedicationEdit> {
                MedicationEditScreen(
                    onSaved = {
                        navController.popBackStack()
                        showSaved()
                    },
                    onArchived = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<History> { HistoryScreen() }
            composable<Settings> {
                SettingsScreen(
                    onBatteryGuide = { navController.navigate(BatteryGuide) },
                    onExport = { navController.navigate(Export) },
                    onProfiles = { navController.navigate(Profiles) },
                    onPro = { navController.navigate(Pro) }
                )
            }
            composable<Profiles> {
                ProfilesScreen(onBack = { navController.popBackStack() }, onOpenPro = { navController.navigate(Pro) })
            }
            composable<BatteryGuide> { BatteryGuideScreen(onBack = { navController.popBackStack() }) }
            composable<Export> {
                ExportScreen(onBack = { navController.popBackStack() }, onOpenPro = { navController.navigate(Pro) })
            }
            composable<Pro> { ProScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        // Today is the root of the back stack once onboarding is done.
        popUpTo<Today> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
