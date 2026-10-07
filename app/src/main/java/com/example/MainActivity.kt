package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.TradingViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TradingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                PunchmanTradingApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PunchmanTradingApp(viewModel: TradingViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 4.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = state.currentTab == AppNavTab.CHART,
                    onClick = { viewModel.selectTab(AppNavTab.CHART) },
                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Chart") },
                    label = { Text("Chart", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = BullGreenBright,
                        indicatorColor = BullGreenBright,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_chart")
                )

                NavigationBarItem(
                    selected = state.currentTab == AppNavTab.SIGNALS,
                    onClick = { viewModel.selectTab(AppNavTab.SIGNALS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (state.signals.isNotEmpty()) {
                                    Badge(containerColor = BullGreen) {
                                        Text("${state.signals.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = "Signals")
                        }
                    },
                    label = { Text("Signals", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = BullGreenBright,
                        indicatorColor = BullGreenBright,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_signals")
                )

                NavigationBarItem(
                    selected = state.currentTab == AppNavTab.MARKETS,
                    onClick = { viewModel.selectTab(AppNavTab.MARKETS) },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Markets") },
                    label = { Text("Markets", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = BullGreenBright,
                        indicatorColor = BullGreenBright,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_markets")
                )

                NavigationBarItem(
                    selected = state.currentTab == AppNavTab.PAPER_TRADING,
                    onClick = { viewModel.selectTab(AppNavTab.PAPER_TRADING) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Paper") },
                    label = { Text("Paper", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = BullGreenBright,
                        indicatorColor = BullGreenBright,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_paper")
                )

                NavigationBarItem(
                    selected = state.currentTab == AppNavTab.ALGO_SETTINGS,
                    onClick = { viewModel.selectTab(AppNavTab.ALGO_SETTINGS) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Algo") },
                    label = { Text("Algo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = BullGreenBright,
                        indicatorColor = BullGreenBright,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_algo")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            when (state.currentTab) {
                AppNavTab.CHART -> {
                    ChartScreen(
                        state = state,
                        onTimeframeSelected = { tf -> viewModel.selectTimeframe(tf) },
                        onTakeTrade = { sig ->
                            viewModel.executePaperTrade(sig)
                            viewModel.selectTab(AppNavTab.PAPER_TRADING)
                        },
                        onNavigateToSettings = { viewModel.selectTab(AppNavTab.ALGO_SETTINGS) }
                    )
                }

                AppNavTab.SIGNALS -> {
                    SignalsScreen(
                        state = state,
                        onTakeTrade = { sig ->
                            viewModel.executePaperTrade(sig)
                            viewModel.selectTab(AppNavTab.PAPER_TRADING)
                        },
                        onRefresh = { viewModel.loadCandlesForSelectedPair() }
                    )
                }

                AppNavTab.MARKETS -> {
                    MarketsScreen(
                        state = state,
                        onPairSelected = { pair ->
                            viewModel.selectPair(pair)
                            viewModel.selectTab(AppNavTab.CHART)
                        },
                        onSearchQueryChanged = { q -> viewModel.updateSearchQuery(q) }
                    )
                }

                AppNavTab.PAPER_TRADING -> {
                    PaperTradingScreen(
                        state = state,
                        onCloseTrade = { tradeId -> viewModel.closeTradeManually(tradeId) },
                        onResetAccount = { viewModel.resetPaperAccount() }
                    )
                }

                AppNavTab.ALGO_SETTINGS -> {
                    SettingsScreen(
                        currentSettings = state.algoSettings,
                        onSaveSettings = { newSettings ->
                            viewModel.updateAlgoSettings(newSettings)
                            viewModel.selectTab(AppNavTab.CHART)
                        }
                    )
                }
            }
        }
    }
}
