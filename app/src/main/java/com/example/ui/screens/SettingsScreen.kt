package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlgoSettings
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    currentSettings: AlgoSettings,
    onSaveSettings: (AlgoSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var fastEma by remember { mutableStateOf(currentSettings.emaFastPeriod.toFloat()) }
    var slowEma by remember { mutableStateOf(currentSettings.emaSlowPeriod.toFloat()) }
    var rsiPeriod by remember { mutableStateOf(currentSettings.rsiPeriod.toFloat()) }
    var rrRatio by remember { mutableStateOf(currentSettings.riskRewardRatio.toFloat()) }
    var showEma by remember { mutableStateOf(currentSettings.showEma) }
    var showBands by remember { mutableStateOf(currentSettings.showBollingerBands) }
    var showVolume by remember { mutableStateOf(currentSettings.showVolume) }
    var showMarkers by remember { mutableStateOf(currentSettings.showBuySellMarkers) }
    var showTargetZones by remember { mutableStateOf(currentSettings.showTargetZones) }
    var sensitivity by remember { mutableStateOf(currentSettings.sensitivity) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Punchman Algo Parameters",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Fine-tune TradingView indicators & signal sensitivity",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sensitivity Preset Selector
        Text("ALGO STRATEGY PRESET", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Aggressive", "Balanced", "Conservative").forEach { preset ->
                val isSelected = sensitivity == preset
                Surface(
                    modifier = Modifier.weight(1f),
                    color = if (isSelected) AccentCyan else SurfaceDark,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    onClick = {
                        sensitivity = preset
                        when (preset) {
                            "Aggressive" -> {
                                fastEma = 7f
                                slowEma = 14f
                                rrRatio = 1.8f
                            }
                            "Balanced" -> {
                                fastEma = 9f
                                slowEma = 21f
                                rrRatio = 2.0f
                            }
                            "Conservative" -> {
                                fastEma = 20f
                                slowEma = 50f
                                rrRatio = 2.5f
                            }
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Indicator Sliders
        Text("INDICATOR PERIODS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Fast EMA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Fast EMA (Trend Entry)", color = TextPrimary, fontSize = 13.sp)
                    Text("${fastEma.toInt()}", color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = fastEma,
                    onValueChange = { fastEma = it },
                    valueRange = 5f..25f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = BorderDark
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Slow EMA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Slow EMA (Baseline Filter)", color = TextPrimary, fontSize = 13.sp)
                    Text("${slowEma.toInt()}", color = AccentGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = slowEma,
                    onValueChange = { slowEma = it },
                    valueRange = 15f..60f,
                    steps = 44,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentGold,
                        activeTrackColor = AccentGold,
                        inactiveTrackColor = BorderDark
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Risk to Reward
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Target Risk/Reward Ratio", color = TextPrimary, fontSize = 13.sp)
                    Text("1 : ${String.format("%.1f", rrRatio)}", color = BullGreenBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = rrRatio,
                    onValueChange = { rrRatio = it },
                    valueRange = 1.0f..3.5f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = BullGreenBright,
                        activeTrackColor = BullGreenBright,
                        inactiveTrackColor = BorderDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Visual Display Toggles
        Text("CHART OVERLAYS & SIGNALS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ToggleRow(
                    title = "Show Buy & Sell Algo Signals",
                    subtitle = "Draw green/red TradingView signal labels",
                    checked = showMarkers,
                    onCheckedChange = { showMarkers = it }
                )
                Divider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

                ToggleRow(
                    title = "Show SL & TP Target Zones",
                    subtitle = "Draw dynamic stop loss and profit targets",
                    checked = showTargetZones,
                    onCheckedChange = { showTargetZones = it }
                )
                Divider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

                ToggleRow(
                    title = "Show EMA Indicator Lines",
                    subtitle = "Fast (Cyan) & Slow (Gold) moving averages",
                    checked = showEma,
                    onCheckedChange = { showEma = it }
                )
                Divider(color = BorderDark, modifier = Modifier.padding(vertical = 10.dp))

                ToggleRow(
                    title = "Show Bollinger Bands",
                    subtitle = "Volatility mean reversion channel",
                    checked = showBands,
                    onCheckedChange = { showBands = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save & Apply Button
        Button(
            onClick = {
                onSaveSettings(
                    currentSettings.copy(
                        emaFastPeriod = fastEma.toInt(),
                        emaSlowPeriod = slowEma.toInt(),
                        riskRewardRatio = rrRatio.toDouble(),
                        showEma = showEma,
                        showBollingerBands = showBands,
                        showVolume = showVolume,
                        showBuySellMarkers = showMarkers,
                        showTargetZones = showTargetZones,
                        sensitivity = sensitivity
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_save_settings"),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Text(
                text = "Apply Settings to Chart & Algo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BullGreen,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceVariantDark
            )
        )
    }
}
