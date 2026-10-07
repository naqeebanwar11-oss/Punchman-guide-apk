package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlgoSignal
import com.example.model.Candle
import com.example.model.SignalType
import com.example.model.Timeframe
import com.example.ui.components.CandlestickChart
import com.example.ui.components.formatDynamicPrice
import com.example.ui.theme.*
import com.example.viewmodel.TradingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    state: TradingUiState,
    onTimeframeSelected: (Timeframe) -> Unit,
    onTakeTrade: (AlgoSignal) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inspectedCandle by remember { mutableStateOf<Candle?>(null) }
    val latestCandle = state.candles.lastOrNull()
    val activeCandle = inspectedCandle ?: latestCandle

    val currentPrice = state.selectedPair.price
    val isPositive24h = state.selectedPair.changePercent24h >= 0
    val activeSignal = state.signals.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top Ticker Bar: Symbol, 24h Change, High/Low, Live Pulse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.selectedPair.displayName,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Realtime Pulse indicator
                    Surface(
                        color = BullGreenBright.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(BullGreenBright, RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                color = BullGreenBright,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${formatDynamicPrice(currentPrice)}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${if (isPositive24h) "+" else ""}${String.format("%.2f", state.selectedPair.changePercent24h)}%",
                        color = if (isPositive24h) BullGreenBright else BearRedBright,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Quick Algo Status Indicator Card
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.clickable { onNavigateToSettings() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Algo Config",
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Algo: ${state.algoSettings.sensitivity}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Timeframe Selector Ribbon (1m, 5m, 15m, 1h, 4h, 1D)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Timeframe.values().forEach { tf ->
                val isSelected = state.selectedTimeframe == tf
                Surface(
                    color = if (isSelected) AccentCyan else SurfaceDark,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable { onTimeframeSelected(tf) }
                        .testTag("timeframe_${tf.label}")
                ) {
                    Text(
                        text = tf.label,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Candle Inspector Legend (O, H, L, C, RSI, EMA)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            activeCandle?.let { c ->
                Text(
                    text = "O: ${formatDynamicPrice(c.open)}",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "H: ${formatDynamicPrice(c.high)}",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "L: ${formatDynamicPrice(c.low)}",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "C: ${formatDynamicPrice(c.close)}",
                    color = if (c.close >= c.open) BullGreenBright else BearRedBright,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                c.rsi?.let { r ->
                    Text(
                        text = "RSI: ${String.format("%.1f", r)}",
                        color = AccentGold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Interactive Candlestick Chart with Buy/Sell Signals
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
        ) {
            CandlestickChart(
                candles = state.candles,
                settings = state.algoSettings,
                onCandleSelected = { candle ->
                    inspectedCandle = candle
                }
            )

            // Legend Overlay (Top Left of chart)
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .background(BackgroundDark.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                    .padding(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(AccentCyan, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EMA 9", color = AccentCyan, fontSize = 9.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).background(AccentGold, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EMA 21", color = AccentGold, fontSize = 9.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live Algo Indicator Banner: Where to BUY / Where to SELL
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("algo_action_banner"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            if (activeSignal != null) {
                val isBuy = activeSignal.type == SignalType.BUY || activeSignal.type == SignalType.STRONG_BUY
                val signalColor = if (isBuy) BullGreenBright else BearRedBright

                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isBuy) BullGreen.copy(alpha = 0.2f) else BearRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isBuy) "BUY ZONE ACTIVE" else "SELL ZONE ACTIVE",
                                    color = signalColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${activeSignal.confidence}% Confidence",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        // Take Trade Action
                        Button(
                            onClick = { onTakeTrade(activeSignal) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBuy) BullGreen else BearRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_chart_take_trade")
                        ) {
                            Text(
                                text = if (isBuy) "Execute BUY" else "Execute SELL",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Targets breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Entry Price", color = TextMuted, fontSize = 10.sp)
                            Text(
                                "$${formatDynamicPrice(activeSignal.entryPrice)}",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("Stop Loss", color = BearRedBright, fontSize = 10.sp)
                            Text(
                                "$${formatDynamicPrice(activeSignal.stopLoss)}",
                                color = BearRedBright,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("Take Profit 1", color = BullGreenBright, fontSize = 10.sp)
                            Text(
                                "$${formatDynamicPrice(activeSignal.target1)}",
                                color = BullGreenBright,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("Take Profit 2", color = AccentGold, fontSize = 10.sp)
                            Text(
                                "$${formatDynamicPrice(activeSignal.target2)}",
                                color = AccentGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Algo currently analyzing real-time volume & momentum. Awaiting confirmation cross.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}
