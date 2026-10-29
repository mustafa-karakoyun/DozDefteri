package app.dozdefteri.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.dozdefteri.R

private data class TopLevelTab(val route: Any, @StringRes val label: Int, val icon: ImageVector)

private val topLevelTabs = listOf(
    TopLevelTab(Today, R.string.tab_today, Icons.Filled.Today),
    TopLevelTab(Medications, R.string.tab_medications, Icons.AutoMirrored.Filled.List),
    TopLevelTab(History, R.string.tab_history, Icons.Filled.CalendarMonth),
    TopLevelTab(Settings, R.string.tab_settings, Icons.Filled.Settings)
)

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = topLevelTabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelTabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
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
            startDestination = Today,
            modifier = Modifier.padding(padding)
        ) {
            composable<Today> { Placeholder(R.string.tab_today) }
            composable<Medications> { Placeholder(R.string.tab_medications) }
            composable<History> { Placeholder(R.string.tab_history) }
            composable<Settings> { Placeholder(R.string.tab_settings) }
        }
    }
}

@Composable
private fun Placeholder(@StringRes title: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(title))
    }
}
