package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NetPulseViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GpsScreen
import com.example.ui.screens.NetworkPingScreen
import com.example.ui.screens.SpeedTestScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.NetPulseTheme
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class NavTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dasbor", Icons.Default.Dashboard),
    PING("Jaringan", Icons.Default.NetworkCheck),
    SPEED_TEST("Speed", Icons.Default.Speed),
    GPS("GPS", Icons.Default.LocationOn),
    TOOLS("Alat", Icons.Default.Build)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetPulseTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: NetPulseViewModel = viewModel()) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }

    // Collect ViewModel states
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val networkTelemetry by viewModel.networkTelemetry.collectAsStateWithLifecycle()
    val gpsTelemetry by viewModel.gpsTelemetry.collectAsStateWithLifecycle()
    val batteryTelemetry by viewModel.batteryTelemetry.collectAsStateWithLifecycle()
    val currentPowerProfile by viewModel.currentPowerProfile.collectAsStateWithLifecycle()
    val isHudShowing by viewModel.isHudShowing.collectAsStateWithLifecycle()

    val speedTestProgress by viewModel.speedTestProgress.collectAsStateWithLifecycle()
    val speedTestHistory by viewModel.speedTestHistory.collectAsStateWithLifecycle()

    val cacheBreakdown by viewModel.cacheBreakdown.collectAsStateWithLifecycle()
    val isCleaningCache by viewModel.isCleaning.collectAsStateWithLifecycle()
    val lastCleanedMb by viewModel.lastCleanedMb.collectAsStateWithLifecycle()

    val isOptimizingRoute by viewModel.isOptimizingRoute.collectAsStateWithLifecycle()
    val routeOptimizationResult by viewModel.routeOptimizationResult.collectAsStateWithLifecycle()

    // Permissions launcher
    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        val needsLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        val needsNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        } else false

        if (needsLocation || needsNotification) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    // Handle back press to return to Dashboard tab
    BackHandler(enabled = currentTab != NavTab.DASHBOARD) {
        currentTab = NavTab.DASHBOARD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = CardSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepNavy,
                            selectedTextColor = SignalGreen,
                            indicatorColor = SignalGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepNavy)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
            ) {
                when (currentTab) {
                    NavTab.DASHBOARD -> DashboardScreen(
                        isServiceRunning = isServiceRunning,
                        networkTelemetry = networkTelemetry,
                        gpsTelemetry = gpsTelemetry,
                        batteryTelemetry = batteryTelemetry,
                        currentPowerProfile = currentPowerProfile,
                        isHudShowing = isHudShowing,
                        lastCleanedMb = lastCleanedMb,
                        onToggleService = { viewModel.toggleMasterOptimizer() },
                        onSelectProfile = { viewModel.selectPowerProfile(it) },
                        onToggleHud = { viewModel.toggleFloatingHud() },
                        onCleanCacheQuick = { viewModel.cleanCache() },
                        onNavigateToSpeedTest = { currentTab = NavTab.SPEED_TEST },
                        onNavigateToPing = { currentTab = NavTab.PING }
                    )

                    NavTab.PING -> NetworkPingScreen(
                        networkTelemetry = networkTelemetry,
                        isOptimizingRoute = isOptimizingRoute,
                        optimizationResult = routeOptimizationResult,
                        onOptimizeRoute = { viewModel.optimizePingRoute() }
                    )

                    NavTab.SPEED_TEST -> SpeedTestScreen(
                        testProgress = speedTestProgress,
                        testHistory = speedTestHistory,
                        onStartTest = { viewModel.runSpeedTest() },
                        onClearHistory = { viewModel.clearSpeedTestHistory() }
                    )

                    NavTab.GPS -> GpsScreen(
                        gpsTelemetry = gpsTelemetry,
                        isServiceRunning = isServiceRunning,
                        onToggleService = { viewModel.toggleMasterOptimizer() }
                    )

                    NavTab.TOOLS -> ToolsScreen(
                        cacheBreakdown = cacheBreakdown,
                        isCleaning = isCleaningCache,
                        lastCleanedMb = lastCleanedMb,
                        batteryTelemetry = batteryTelemetry,
                        isHudShowing = isHudShowing,
                        onCleanCache = { viewModel.cleanCache() },
                        onToggleHud = { viewModel.toggleFloatingHud() },
                        onRequestPermissions = { permissionLauncher.launch(permissionsToRequest) }
                    )
                }
            }
        }
    }
}
