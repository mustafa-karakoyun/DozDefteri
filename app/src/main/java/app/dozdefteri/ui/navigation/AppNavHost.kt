package app.dozdefteri.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.core.format.TurkishSuffix
import app.dozdefteri.ui.components.BottomTab
import app.dozdefteri.ui.components.DozBottomBar
import app.dozdefteri.ui.components.DozNavIcons
import app.dozdefteri.ui.components.HeaderProfile
import app.dozdefteri.ui.shell.LocalHeaderProfile
import app.dozdefteri.ui.shell.ProfileSwitcherSheet
import app.dozdefteri.ui.shell.ShellViewModel
import app.dozdefteri.ui.theme.Doz
import kotlinx.coroutines.launch

private data class TopLevelTab(val route: Any, @StringRes val label: Int, val icon: ImageVector)

private val topLevelTabs = listOf(
    TopLevelTab(Today, R.string.tab_today, DozNavIcons.Today),
    TopLevelTab(Medications, R.string.tab_medications, DozNavIcons.Medications),
    TopLevelTab(History, R.string.tab_history, DozNavIcons.History),
    TopLevelTab(Settings, R.string.tab_settings, DozNavIcons.Settings)
)

@Composable
fun AppNavHost(
    onboardingDone: Boolean,
    pendingDestination: String?,
    onDestinationHandled: () -> Unit,
    navController: NavHostController = rememberNavController(),
    shellViewModel: ShellViewModel = hiltViewModel()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = topLevelTabs.any { tab ->
        destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Fixed for the lifetime of the host: changing it later would reset the back stack.
    val startDestination: Any = remember { if (onboardingDone) Today else Onboarding }
    val savedMessage = stringResource(R.string.saved)
    val showSaved: () -> Unit = { scope.launch { snackbarHostState.showSnackbar(savedMessage) } }
    val switchedFormat = stringResource(R.string.switcher_switched)

    val activeProfile by shellViewModel.activeProfile.collectAsStateWithLifecycle()
    val summaries by shellViewModel.summaries.collectAsStateWithLifecycle()
    var switcherOpen by rememberSaveable { mutableStateOf(false) }
    val headerProfile = activeProfile?.let { profile ->
        HeaderProfile(profile.name, profile.colorIndex) {
            shellViewModel.loadSummaries()
            switcherOpen = true
        }
    }

    LaunchedEffect(pendingDestination, onboardingDone) {
        if (pendingDestination == null || !onboardingDone) return@LaunchedEffect
        when (pendingDestination) {
            MainActivity.DESTINATION_TODAY -> navController.navigateToTab(Today)
            MainActivity.DESTINATION_EXPORT -> navController.navigate(Export)
            MainActivity.DESTINATION_PRO -> navController.navigate(Pro)
        }
        onDestinationHandled()
    }

    if (switcherOpen) {
        ProfileSwitcherSheet(
            summaries = summaries,
            activeId = activeProfile?.id,
            onSelect = { profile ->
                switcherOpen = false
                if (profile.id != activeProfile?.id) {
                    shellViewModel.switchTo(profile.id)
                    val owner = if (TurkishSuffix.isTurkish()) {
                        TurkishSuffix.nameGenitive(profile.name)
                    } else {
                        profile.name
                    }
                    scope.launch { snackbarHostState.showSnackbar(String.format(switchedFormat, owner)) }
                }
            },
            onManage = {
                switcherOpen = false
                navController.navigate(Profiles)
            },
            onDismiss = { switcherOpen = false }
        )
    }

    CompositionLocalProvider(LocalHeaderProfile provides headerProfile) {
        Scaffold(
            containerColor = Doz.colors.bgApp,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    DozBottomBar(
                        topLevelTabs.map { tab ->
                            BottomTab(
                                label = stringResource(tab.label),
                                icon = tab.icon,
                                selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true,
                                onClick = { navController.navigateToTab(tab.route) }
                            )
                        }
                    )
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
                    ProfilesScreen(
                        onBack = { navController.popBackStack() },
                        onOpenPro = { navController.navigate(Pro) }
                    )
                }
                composable<BatteryGuide> { BatteryGuideScreen(onBack = { navController.popBackStack() }) }
                composable<Export> {
                    ExportScreen(onBack = { navController.popBackStack() }, onOpenPro = { navController.navigate(Pro) })
                }
                composable<Pro> { ProScreen(onBack = { navController.popBackStack() }) }
            }
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
