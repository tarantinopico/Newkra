package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DoseModalDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StockScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface1
import com.example.ui.theme.ColorSuccess
import com.example.ui.theme.ColorWarning
import com.example.ui.theme.KratomTrackerTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appState by viewModel.state.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val doseModalOpen by viewModel.doseModalOpen.collectAsState()
            val editingDose by viewModel.editingDose.collectAsState()

            KratomTrackerTheme(
                themeName = appState.settings.theme,
                customAccentHex = appState.settings.customAccentHex
            ) {
                // Back button handling
                if (currentTab != "dashboard") {
                    BackHandler {
                        viewModel.navigateTo("dashboard")
                    }
                }

                Scaffold(
                    topBar = {
                        if (currentTab != "settings") {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Kratom Tracker",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp
                                        )
                                    }
                                },
                                actions = {
                                    // Privacy mode toggle
                                    IconButton(
                                        onClick = { viewModel.togglePrivacyMode() },
                                        modifier = Modifier.testTag("privacy_mode_toggle")
                                    ) {
                                        Icon(
                                            imageVector = if (appState.settings.privacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Režim soukromí",
                                            tint = if (appState.settings.privacyMode) ColorWarning else TextSecondary
                                        )
                                    }

                                    // Settings button
                                    IconButton(
                                        onClick = { viewModel.navigateTo("settings") },
                                        modifier = Modifier.testTag("settings_nav_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Nastavení",
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = AmoledBlack)
                            )
                        }
                    },
                    bottomBar = {
                        if (currentTab != "settings") {
                            CustomBottomNavigation(
                                currentTab = currentTab,
                                onSelectTab = { viewModel.navigateTo(it) },
                                onOpenFab = { viewModel.openNewDoseModal() }
                            )
                        }
                    },
                    containerColor = AmoledBlack,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            "dashboard" -> {
                                DashboardScreen(
                                    appState = appState,
                                    onOpenDoseModal = { dose ->
                                        if (dose != null) viewModel.openEditDoseModal(dose) else viewModel.openNewDoseModal()
                                    },
                                    onQuickDose = { viewModel.quickDose(it) },
                                    onRepeatLastDose = { viewModel.repeatLastDose() },
                                    onQuickWater = { viewModel.quickWater(it) },
                                    onStartTBreak = { viewModel.startTBreak() },
                                    onFinishTBreak = { viewModel.finishTBreak() },
                                    onNavigateToHistory = { viewModel.navigateTo("history") },
                                    onNavigateToAchievements = { viewModel.navigateTo("analytics") }
                                )
                            }
                            "stock" -> {
                                StockScreen(
                                    appState = appState,
                                    onAddPackage = { viewModel.addPackage(it) },
                                    onUpdatePackage = { viewModel.updatePackage(it) },
                                    onDeletePackage = { viewModel.deletePackage(it) },
                                    onOpenPackage = { viewModel.openPackage(it) },
                                    onReconcilePackage = { pkg, tw, method, ids ->
                                        viewModel.reconcilePackage(pkg, tw, method, ids)
                                    },
                                    onUndoReconciliation = { viewModel.undoReconciliation(it) }
                                )
                            }
                            "history" -> {
                                HistoryScreen(
                                    appState = appState,
                                    onOpenDoseModal = { viewModel.openEditDoseModal(it) },
                                    onDeleteDose = { viewModel.deleteDose(it) },
                                    onRestoreDose = { viewModel.restoreDose(it) }
                                )
                            }
                            "analytics" -> {
                                AnalyticsScreen(
                                    appState = appState,
                                    onStartTBreak = { viewModel.startTBreak() }
                                )
                            }
                            "settings" -> {
                                SettingsScreen(
                                    appState = appState,
                                    onUpdateSettings = { viewModel.updateSettings(it) },
                                    onAddStrain = { viewModel.addStrain(it) },
                                    onUpdateStrain = { viewModel.updateStrain(it) },
                                    onDeleteStrain = { viewModel.deleteStrain(it) },
                                    onAddBeverage = { viewModel.addBeverage(it) },
                                    onUpdateBeverage = { viewModel.updateBeverage(it) },
                                    onDeleteBeverage = { viewModel.deleteBeverage(it) },
                                    onExportJson = { viewModel.exportJson() },
                                    onExportCsv = { viewModel.exportCsv() },
                                    onImportJson = { viewModel.importJson(it) },
                                    onResetAllData = { viewModel.resetAllData() },
                                    onBack = { viewModel.navigateTo("dashboard") }
                                )
                            }
                        }

                        // Fullscreen dose modal dialog
                        if (doseModalOpen) {
                            DoseModalDialog(
                                initialDose = editingDose,
                                appState = appState,
                                onDismiss = { viewModel.closeDoseModal() },
                                onSave = { viewModel.saveDose(it) },
                                onDelete = { viewModel.deleteDose(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(
    currentTab: String,
    onSelectTab: (String) -> Unit,
    onOpenFab: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface1)
            .border(width = 0.5.dp, color = CardBorder)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Domů
            NavItem(
                icon = Icons.Default.Home,
                label = "Domů",
                isSelected = currentTab == "dashboard",
                onClick = { onSelectTab("dashboard") },
                modifier = Modifier.weight(1f)
            )

            // 2. Sklad
            NavItem(
                icon = Icons.Default.Inventory2,
                label = "Sklad",
                isSelected = currentTab == "stock",
                onClick = { onSelectTab("stock") },
                modifier = Modifier.weight(1f)
            )

            // 3. Central FAB (+)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onOpenFab,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("center_fab_add_dose")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zapsat dávku",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 4. Historie
            NavItem(
                icon = Icons.Default.History,
                label = "Historie",
                isSelected = currentTab == "history",
                onClick = { onSelectTab("history") },
                modifier = Modifier.weight(1f)
            )

            // 5. Přehled
            NavItem(
                icon = Icons.Default.BarChart,
                label = "Přehled",
                isSelected = currentTab == "analytics",
                onClick = { onSelectTab("analytics") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else TextMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = if (isSelected) MaterialTheme.colorScheme.primary else TextMuted,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
