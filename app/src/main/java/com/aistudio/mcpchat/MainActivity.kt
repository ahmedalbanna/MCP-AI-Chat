package com.aistudio.mcpchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aistudio.mcpchat.data.local.ThemeMode
import com.aistudio.mcpchat.data.model.McpServer
import com.aistudio.mcpchat.ui.adaptive.SecondaryPane
import com.aistudio.mcpchat.ui.adaptive.horizontalHingeInsetPx
import com.aistudio.mcpchat.ui.adaptive.rememberHingeInfo
import com.aistudio.mcpchat.ui.adaptive.twoPaneSplitFraction
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

private fun paneForRoute(route: String): SecondaryPane? = when (route) {
    Screen.Servers.route -> SecondaryPane.SERVERS
    Screen.Inspector.route -> SecondaryPane.INSPECTOR
    Screen.Settings.route -> SecondaryPane.SETTINGS
    else -> null
}

private fun routeForPane(pane: SecondaryPane): String? = when (pane) {
    SecondaryPane.NONE -> null
    SecondaryPane.SERVERS -> Screen.Servers.route
    SecondaryPane.INSPECTOR -> Screen.Inspector.route
    SecondaryPane.SETTINGS -> Screen.Settings.route
}

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
    LaunchedEffect(Unit) {
        showOnboarding = !hasSeenOnboarding(context)
    }
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Activity-scoped so chat, the providers list and the provider editor all
    // share one draft/active-provider state.
    val aiProviderViewModel: AiProviderSettingsViewModel = hiltViewModel()

    val activity = context as? ComponentActivity

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    val widthSizeClass = activity
        ?.let { calculateWindowSizeClass(it).widthSizeClass }
        ?: WindowWidthSizeClass.Compact
    val useRail = widthSizeClass != WindowWidthSizeClass.Compact
    val isTwoPane = widthSizeClass == WindowWidthSizeClass.Expanded
    val hinge = rememberHingeInfo()

    // Which auxiliary destination shares the screen with chat (expanded only).
    var secondaryPane by rememberSaveable { mutableStateOf(SecondaryPane.NONE) }

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

    // Expanding with a secondary destination open (e.g. unfold while on the
    // servers list): carry it into the side pane and pop chat back on top of
    // the primary NavHost.
    LaunchedEffect(isTwoPane, currentRoute) {
        if (isTwoPane) {
            val pane = currentRoute?.let(::paneForRoute) ?: return@LaunchedEffect
            secondaryPane = pane
            navController.popBackStack(Screen.Chat.route, inclusive = false)
        }
    }

    // Collapsing with a pane open: restore it as a destination so the user
    // doesn't lose the screen they were on.
    LaunchedEffect(isTwoPane, secondaryPane) {
        if (!isTwoPane && secondaryPane != SecondaryPane.NONE) {
            routeForPane(secondaryPane)?.let { route -> navigateTopLevel(navController, route) }
            secondaryPane = SecondaryPane.NONE
        }
    }

    // System back closes the side pane before leaving the app.
    BackHandler(enabled = isTwoPane && secondaryPane != SecondaryPane.NONE) {
        secondaryPane = SecondaryPane.NONE
    }

    val openServers: () -> Unit = {
        if (isTwoPane) {
            secondaryPane = SecondaryPane.SERVERS
        } else {
            navigateTopLevel(navController, Screen.Servers.route)
        }
    }

    Scaffold(
        bottomBar = {
            if (!useRail) {
                AnimatedVisibility(visible = isTopLevelRoute) {
                    AppBottomBar(
                        items = items,
                        currentRoute = currentRoute,
                        onNavigate = { screen -> navigateTopLevel(navController, screen.route) },
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Compact keeps the bottom bar; medium+ switch to a rail. In
            // two-pane the rail stays put while forms open in the chat pane.
            if (useRail && (isTopLevelRoute || isTwoPane)) {
                AppNavigationRail(
                    items = items,
                    currentRoute = currentRoute,
                    isTwoPane = isTwoPane,
                    secondaryPane = secondaryPane,
                    onNavigate = { screen -> navigateTopLevel(navController, screen.route) },
                    onSelectPane = { secondaryPane = it },
                )
            }

            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val paneAreaWidthPx = constraints.maxWidth.toFloat()
                val paneAreaHeightPx = constraints.maxHeight.toFloat()
                val splitFraction = if (isTwoPane && secondaryPane != SecondaryPane.NONE) {
                    twoPaneSplitFraction(paneAreaWidthPx, hinge)
                } else {
                    1f
                }
                val density = LocalDensity.current
                val hingeGap = if (hinge != null && hinge.isVertical) {
                    with(density) { hinge.widthPx.toDp() / 2 }
                } else {
                    0.dp
                }
                val (hingeTopPx, hingeBottomPx) = horizontalHingeInsetPx(paneAreaHeightPx, hinge)
                val hingeTopGap = with(density) { hingeTopPx.toDp() }
                val hingeBottomGap = with(density) { hingeBottomPx.toDp() }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = hingeTopGap, bottom = hingeBottomGap),
                ) {
                    // Stable call position: the NavHost lives here in every
                    // width mode so fold/resize never resets its back stack.
                    Box(modifier = Modifier.weight(splitFraction)) {
                        AppNavGraph(
                            navController = navController,
                            aiProviderViewModel = aiProviderViewModel,
                            onOpenServers = openServers,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = if (splitFraction < 1f) hingeGap else 0.dp),
                        )
                    }

                    if (splitFraction < 1f) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = hingeTopGap)
                                .width(1.dp),
                        )
                        Box(modifier = Modifier.weight(1f - splitFraction)) {
                            SecondaryPaneContent(
                                pane = secondaryPane,
                                onAddServer = { navController.navigate(SERVER_ADD_ROUTE) },
                                onEditServer = { server -> navController.navigate(serverEditRoute(server.id)) },
                                onOpenServers = openServers,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = hingeGap),
                            )
                        }
                    }
                }
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

private fun navigateTopLevel(navController: NavController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTextColor = IndigoPrimary,
    indicatorColor = IndigoPrimary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun railItemColors() = NavigationRailItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTextColor = IndigoPrimary,
    indicatorColor = IndigoPrimary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun AppBottomBar(items: List<Screen>, currentRoute: String?, onNavigate: (Screen) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("bottom_navigation_bar"),
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                selected = currentRoute == screen.route,
                colors = navItemColors(),
                modifier = Modifier.testTag("nav_tab_${screen.route}"),
                onClick = { onNavigate(screen) },
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    items: List<Screen>,
    currentRoute: String?,
    isTwoPane: Boolean,
    secondaryPane: SecondaryPane,
    onNavigate: (Screen) -> Unit,
    onSelectPane: (SecondaryPane) -> Unit,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("navigation_rail"),
    ) {
        items.forEach { screen ->
            // In two-pane, chat itself is the primary surface: tapping it
            // dismisses the side pane; the others swap the pane's content.
            val targetPane = paneForRoute(screen.route) ?: SecondaryPane.NONE
            val selected = if (isTwoPane) {
                secondaryPane == targetPane
            } else {
                currentRoute == screen.route
            }
            NavigationRailItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                selected = selected,
                colors = railItemColors(),
                modifier = Modifier.testTag("nav_tab_${screen.route}"),
                onClick = {
                    if (isTwoPane) onSelectPane(targetPane) else onNavigate(screen)
                },
            )
        }
    }
}

@Composable
private fun SecondaryPaneContent(
    pane: SecondaryPane,
    onAddServer: () -> Unit,
    onEditServer: (McpServer) -> Unit,
    onOpenServers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (pane) {
        SecondaryPane.NONE -> Unit
        SecondaryPane.SERVERS -> {
            val serversViewModel: com.aistudio.mcpchat.ui.viewmodel.McpServersViewModel = hiltViewModel()
            ServersScreen(
                viewModel = serversViewModel,
                onAddServer = onAddServer,
                onEditServer = onEditServer,
                modifier = modifier,
            )
        }
        SecondaryPane.INSPECTOR -> {
            val inspectorViewModel: com.aistudio.mcpchat.ui.viewmodel.McpInspectorViewModel = hiltViewModel()
            InspectorScreen(
                viewModel = inspectorViewModel,
                onNavigateToServers = onOpenServers,
                modifier = modifier,
            )
        }
        SecondaryPane.SETTINGS -> {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            SettingsScreen(viewModel = themeViewModel, modifier = modifier)
        }
    }
}

@Composable
private fun AppNavGraph(
    navController: NavHostController,
    aiProviderViewModel: AiProviderSettingsViewModel,
    onOpenServers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route,
        enterTransition = { fadeIn() + slideInVertically(initialOffsetY = { it / 40 }) },
        exitTransition = { fadeOut() + slideOutVertically(targetOffsetY = { it / 40 }) },
        modifier = modifier,
    ) {
        composable(Screen.Chat.route) {
            val chatViewModel: com.aistudio.mcpchat.ui.viewmodel.ChatViewModel = hiltViewModel()
            ChatScreen(
                viewModel = chatViewModel,
                providerViewModel = aiProviderViewModel,
                onNavigateToServers = onOpenServers,
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
                onNavigateToServers = onOpenServers,
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
