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
import androidx.compose.material.icons.filled.Chat
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aistudio.mcpchat.data.local.ThemeMode
import com.aistudio.mcpchat.ui.screens.ChatScreen
import com.aistudio.mcpchat.ui.screens.InspectorScreen
import com.aistudio.mcpchat.ui.screens.OnboardingScreen
import com.aistudio.mcpchat.ui.screens.ServersScreen
import com.aistudio.mcpchat.ui.screens.SettingsScreen
import com.aistudio.mcpchat.ui.screens.hasSeenOnboarding
import com.aistudio.mcpchat.ui.screens.markOnboardingSeen
import com.aistudio.mcpchat.ui.theme.IndigoPrimary
import com.aistudio.mcpchat.ui.theme.MyApplicationTheme
import com.aistudio.mcpchat.ui.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object Servers : Screen("servers", "MCP Servers", Icons.Default.Hub)
    object Inspector : Screen("inspector", "Inspector", Icons.Default.Science)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
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
    androidx.compose.runtime.LaunchedEffect(Unit) {
        showOnboarding = !hasSeenOnboarding(context)
    }
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = remember {
        listOf(
            Screen.Chat,
            Screen.Servers,
            Screen.Inspector,
            Screen.Settings,
        )
    }

    Scaffold(
        bottomBar = {
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
                val providerViewModel: com.aistudio.mcpchat.ui.viewmodel.AiProviderSettingsViewModel = hiltViewModel()
                ChatScreen(
                    viewModel = chatViewModel,
                    providerViewModel = providerViewModel,
                    onNavigateToServers = {
                        navController.navigate(Screen.Servers.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Screen.Servers.route) {
                val serversViewModel: com.aistudio.mcpchat.ui.viewmodel.McpServersViewModel = hiltViewModel()
                ServersScreen(viewModel = serversViewModel)
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
