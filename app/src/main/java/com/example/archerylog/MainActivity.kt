package com.example.archerylog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import com.example.archerylog.ui.ArcheryViewModel
import com.example.archerylog.ui.screens.SplashScreen
import com.example.archerylog.ui.screens.LoginScreen
import com.example.archerylog.ui.screens.RecordsScreen
import com.example.archerylog.ui.screens.LogSessionScreen
import com.example.archerylog.ui.screens.SessionDetailsScreen
import com.example.archerylog.ui.screens.StatisticsScreen
import com.example.archerylog.ui.screens.AccountScreen
import com.example.archerylog.ui.screens.AddSessionScreen
import com.example.archerylog.ui.theme.ArcheryLogTheme
import com.example.archerylog.ui.utils.L10n
import com.example.archerylog.data.LocationType
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    
    private val viewModel: ArcheryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Completely disable activity-level transitions
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        
        setContent {
            ArcheryLogTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ArcheryApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun ArcheryApp(viewModel: ArcheryViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val l10n = L10n(currentLanguage)

    val bottomBarRoutes = listOf("dashboard", "records", "add_session", "account")
    val isSyncing by viewModel.isSyncing.collectAsState()

    val instantEnter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(0))
    val instantExit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(0))

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .fillMaxWidth()
                            .height(64.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All navigation actions set to launchSingleTop and restoreState for tab-like behavior
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    navController.navigate("dashboard") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Home, null, tint = if (currentRoute == "dashboard") MaterialTheme.colorScheme.primary else Color.LightGray)
                                Text(l10n.navHome, fontSize = 12.sp, color = if (currentRoute == "dashboard") MaterialTheme.colorScheme.primary else Color.LightGray)
                            }
                        }
                        
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    navController.navigate("records") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EventNote, null, tint = if (currentRoute == "records") MaterialTheme.colorScheme.primary else Color.LightGray)
                                Text(l10n.navRecords, fontSize = 12.sp, color = if (currentRoute == "records") MaterialTheme.colorScheme.primary else Color.LightGray)
                            }
                        }

                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    navController.navigate("add_session") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddCircle, null, tint = if (currentRoute == "add_session") MaterialTheme.colorScheme.primary else Color.LightGray)
                                Text(l10n.navAdd, fontSize = 12.sp, color = if (currentRoute == "add_session") MaterialTheme.colorScheme.primary else Color.LightGray)
                            }
                        }

                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    navController.navigate("account") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Person, null, tint = if (currentRoute == "account") MaterialTheme.colorScheme.primary else Color.LightGray)
                                Text(l10n.navAccount, fontSize = 12.sp, color = if (currentRoute == "account") MaterialTheme.colorScheme.primary else Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding())) {
            NavHost(
                navController = navController, 
                startDestination = "splash",
                modifier = Modifier.fillMaxSize(),
                enterTransition = { instantEnter },
                exitTransition = { instantExit },
                popEnterTransition = { instantEnter },
                popExitTransition = { instantExit }
            ) {
                composable("splash", enterTransition = { instantEnter }, exitTransition = { instantExit }) {
                    SplashScreen(onTimeout = {
                        val dest = if (viewModel.isLoggedIn) "dashboard" else "login"
                        navController.navigate(dest) { popUpTo("splash") { inclusive = true } }
                    })
                }
                composable("login", enterTransition = { instantEnter }, exitTransition = { instantExit }) {
                    LoginScreen(viewModel = viewModel, onLoginSuccess = {
                        navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                    })
                }
                composable("dashboard", enterTransition = { instantEnter }, exitTransition = { instantExit }) {
                    StatisticsScreen(viewModel = viewModel)
                }
                composable("records", enterTransition = { instantEnter }, exitTransition = { instantExit }, popEnterTransition = { instantEnter }, popExitTransition = { instantExit }) {
                    RecordsScreen(viewModel = viewModel, onAddSessionClick = { navController.navigate("add_session") }, onSessionClick = { id -> navController.navigate("session_details/$id") })
                }
                composable("add_session", enterTransition = { instantEnter }, exitTransition = { instantExit }, popEnterTransition = { instantEnter }, popExitTransition = { instantExit }) {
                    AddSessionScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onStartSession = { _, _ -> /* Unified screen handles this now */ })
                }
                composable("account", enterTransition = { instantEnter }, exitTransition = { instantExit }) {
                    AccountScreen(viewModel = viewModel, onLogout = { navController.navigate("login") { popUpTo(navController.graph.id) { inclusive = true } } })
                }
                composable("session_details/{sessionId}", enterTransition = { instantEnter }, exitTransition = { instantExit }) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("sessionId")
                    if (id != null) SessionDetailsScreen(viewModel = viewModel, sessionId = id, onBack = { navController.popBackStack() })
                }
            }

            if (isSyncing) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
