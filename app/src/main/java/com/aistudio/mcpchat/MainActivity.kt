package com.aistudio.mcpchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aistudio.mcpchat.data.local.ThemeMode
import com.aistudio.mcpchat.ui.screens.ChatScreen
import com.aistudio.mcpchat.ui.screens.InspectorScreen
import com.aistudio.mcpchat.ui.screens.OnboardingScreen
import com.aistudio.mcpchat.ui.screens.ProviderEditScreen
import com.aistudio.mcpchat.ui.screens.ProvidersScreen
import com.aistudio.mcpchat.ui.screens.ServerEditScreen
import com.aistudio.mcpchat.ui.screens.ServersScreen
import com.aistudio.mcpchat.ui.screens.SettingsScreen
import com.aistudio.mcpchat.ui.screens.hasSeenOnboarding
import com.aistudio.mcpchat.ui.screens.markOnboardingSeen
import com.aistudio.mcpchat.ui.theme.IndigoPrimary
import com.aistudio.mcpchat.ui.theme.MyApplicationTheme
import com.aistudio.mcpchat.ui.viewmodel.AiProviderSettingsViewModel
import com.aistudio.mcpchat.ui.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.AutoMirrored.Default.Chat)
    object Servers : Screen("servers", "MCP Servers", Icons.Default.Hub)
    object Inspector : Screen("inspector", "Inspector", Icons.Default.Science)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

/** Sub-routes pushed on top of the bottom-bar destinations. */
private const val SERVER_ADD_ROUTE = "servers/add"
private const val SERVER_EDIT_ROUTE = "servers/edit/{serverId}"
private fun serverEditRoute(serverId: String) = "servers/edit/$serverId"

private const val PROVIDERS_ROUTE = "providers"
private const val PROVIDER_EDIT_ROUTE = "providers/edit?configId={configId}&presetType={presetType}"
private fun providerAddRoute(presetType: String) = "providers/edit?presetType=$presetType"
private fun providerEditRoute(configId: String) = "providers/edit?configId=$configId"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val prefs by themeViewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (prefs.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }
            MyApplicationTheme(
                darkTheme = darkTheme,
                dynamicColor = prefs.useDynamicColor,
                useAmoledDark = prefs.useAmoledDark,
            ) {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    var showOnboarding by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        showOnboarding = !hasSeenOnboarding(context)
    }
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Activity-scoped so chat, the providers list and the provider editor all
    // share one draft/active-provider state.
    val aiProviderViewModel: AiProviderSettingsViewModel = hiltViewModel()

    val items = remember {
        listOf(
            Screen.Chat,
            Screen.Servers,
            Screen.Inspector,
            Screen.Settings,
        )
    }

    // Sub-screens (add/edit server) replace the whole screen: hide the bottom
    // bar so the form gets the full viewport while entering values.
    val isTopLevelRoute = currentRoute == null || items.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = isTopLevelRoute) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_navigation_bar"),
                ) {
                    items.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(screen.icon, contentDescription = screen.title)
                            },
                            label = { Text(screen.title, fontSize = 11.sp) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = IndigoPrimary,
                                indicatorColor = IndigoPrimary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.testTag("nav_tab_${screen.route}"),
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            enterTransition = { fadeIn() + slideInVertically(initialOffsetY = { it / 40 }) },
            exitTransition = { fadeOut() + slideOutVertically(targetOffsetY = { it / 40 }) },
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Chat.route) {
                val chatViewModel: com.aistudio.mcpchat.ui.viewmodel.ChatViewModel = hiltViewModel()
                ChatScreen(
                    viewModel = chatViewModel,
                    providerViewModel = aiProviderViewModel,
                    onNavigateToServers = {
                        navController.navigate(Screen.Servers.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProviders = { navController.navigate(PROVIDERS_ROUTE) },
                )
            }
            composable(Screen.Servers.route) {
                val serversViewModel: com.aistudio.mcpchat.ui.viewmodel.McpServersViewModel = hiltViewModel()
                ServersScreen(
                    viewModel = serversViewModel,
                    onAddServer = { navController.navigate(SERVER_ADD_ROUTE) },
                    onEditServer = { server -> navController.navigate(serverEditRoute(server.id)) },
                )
            }
            composable(SERVER_ADD_ROUTE) {
                val serversViewModel: com.aistudio.mcpchat.ui.viewmodel.McpServersViewModel = hiltViewModel()
                ServerEditScreen(
                    viewModel = serversViewModel,
                    serverId = null,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(SERVER_EDIT_ROUTE) { backStackEntry ->
                val serversViewModel: com.aistudio.mcpchat.ui.viewmodel.McpServersViewModel = hiltViewModel()
                ServerEditScreen(
                    viewModel = serversViewModel,
                    serverId = backStackEntry.arguments?.getString("serverId"),
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.Inspector.route) {
                val inspectorViewModel: com.aistudio.mcpchat.ui.viewmodel.McpInspectorViewModel = hiltViewModel()
                InspectorScreen(
                    viewModel = inspectorViewModel,
                    onNavigateToServers = {
                        navController.navigate(Screen.Servers.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Screen.Settings.route) {
                val themeViewModel: com.aistudio.mcpchat.ui.viewmodel.ThemeViewModel = hiltViewModel()
                SettingsScreen(viewModel = themeViewModel)
            }
            composable(PROVIDERS_ROUTE) {
                ProvidersScreen(
                    viewModel = aiProviderViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onAddProvider = { type -> navController.navigate(providerAddRoute(type.name)) },
                    onEditProvider = { configId -> navController.navigate(providerEditRoute(configId)) },
                )
            }
            composable(
                route = PROVIDER_EDIT_ROUTE,
                arguments = listOf(
                    navArgument("configId") {
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("presetType") {
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                ProviderEditScreen(
                    viewModel = aiProviderViewModel,
                    configId = backStackEntry.arguments?.getString("configId"),
                    presetType = backStackEntry.arguments?.getString("presetType"),
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }

    AnimatedVisibility(
        visible = showOnboarding,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        OnboardingScreen(
            onDismiss = {
                markOnboardingSeen(context)
                showOnboarding = false
            },
        )
    }
}
