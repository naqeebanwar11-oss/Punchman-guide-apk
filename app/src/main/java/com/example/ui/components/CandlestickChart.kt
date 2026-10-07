package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlgoSettings
import com.example.model.Candle
import com.example.model.SignalType
import com.example.ui.theme.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    settings: AlgoSettings,
    modifier: Modifier = Modifier,
    onCandleSelected: (Candle?) -> Unit = {}
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Loading real-time market data...",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(candles) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val visibleCount = min(candles.size, 50)
                            val candleWidth = size.width / visibleCount
                            val idx = (offset.x / candleWidth).toInt().coerceIn(0, visibleCount - 1)
                            val realIdx = candles.size - visibleCount + idx
                            if (realIdx in candles.indices) {
                                selectedIndex = realIdx
                                onCandleSelected(candles[realIdx])
                            }
                        },
                        onDrag = { change, _ ->
                            val visibleCount = min(candles.size, 50)
                            val candleWidth = size.width / visibleCount
                            val idx = (change.position.x / candleWidth).toInt().coerceIn(0, visibleCount - 1)
                            val realIdx = candles.size - visibleCount + idx
                            if (realIdx in candles.indices) {
                                selectedIndex = realIdx
                                onCandleSelected(candles[realIdx])
                            }
                        },
                        onDragEnd = {
                            // keep last selected or reset
                        }
                    )
                }
                .pointerInput(candles) {
                    detectTapGestures { offset ->
                        val visibleCount = min(candles.size, 50)
                        val candleWidth = size.width / visibleCount
                        val idx = (offset.x / candleWidth).toInt().coerceIn(0, visibleCount - 1)
                        val realIdx = candles.size - visibleCount + idx
                        if (realIdx in candles.indices) {
                            selectedIndex = if (selectedIndex == realIdx) null else realIdx
                            onCandleSelected(if (selectedIndex != null) candles[realIdx] else null)
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Chart area split: 75% for Candlesticks & Target Zones, 25% for Volume / Sub-indicator
            val mainChartHeight = canvasHeight * 0.76f
            val volumeHeight = canvasHeight * 0.20f
            val volumeTop = canvasHeight * 0.80f

            // Visible candles (latest 45 to 55 candles)
            val visibleCount = min(candles.size, 45)
            val visibleCandles = candles.takeLast(visibleCount)
            if (visibleCandles.isEmpty()) return@Canvas

            var minPrice = visibleCandles.minOf { it.low }
            var maxPrice = visibleCandles.maxOf { it.high }

            // Include Bollinger bands or indicators in scale if visible
            if (settings.showBollingerBands) {
                visibleCandles.forEach { c ->
                    c.upperBand?.let { maxPrice = max(maxPrice, it) }
                    c.lowerBand?.let { minPrice = min(minPrice, it) }
                }
            }

            // Margin padding for price range
            val priceRange = max(maxPrice - minPrice, 0.0001)
            val paddedMin = minPrice - (priceRange * 0.05)
            val paddedMax = maxPrice + (priceRange * 0.05)
            val paddedRange = paddedMax - paddedMin

            val maxVolume = visibleCandles.maxOfOrNull { it.volume } ?: 1.0

            fun priceToY(price: Double): Float {
                val ratio = (paddedMax - price) / paddedRange
                return (ratio * mainChartHeight).toFloat().coerceIn(10f, mainChartHeight)
            }

            val candleWidth = canvasWidth / visibleCount
            val bodyWidth = candleWidth * 0.65f

            // 1. Draw Subtle Grid Lines & Horizontal Price levels
            val gridSteps = 4
            for (step in 0..gridSteps) {
                val gridY = (mainChartHeight / gridSteps) * step
                val priceAtGrid = paddedMax - (paddedRange / gridSteps) * step

                drawLine(
                    color = BorderDark.copy(alpha = 0.5f),
                    start = Offset(0f, gridY),
                    end = Offset(canvasWidth, gridY),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw price label on the right side
                val priceStr = formatDynamicPrice(priceAtGrid)
                val textLayout = textMeasurer.measure(
                    text = priceStr,
                    style = TextStyle(color = TextMuted, fontSize = 9.sp)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = priceStr,
                    topLeft = Offset(canvasWidth - textLayout.size.width - 8f, gridY - 14f),
                    style = TextStyle(color = TextMuted, fontSize = 9.sp)
                )
            }

            // 2. Draw Bollinger Bands Cloud / Lines
            if (settings.showBollingerBands) {
                val upperPath = Path()
                val lowerPath = Path()
                var started = false

                visibleCandles.forEachIndexed { i, c ->
                    if (c.upperBand != null && c.lowerBand != null) {
                        val x = i * candleWidth + (candleWidth / 2f)
                        val yUp = priceToY(c.upperBand)
                        val yLow = priceToY(c.lowerBand)

                        if (!started) {
                            upperPath.moveTo(x, yUp)
                            lowerPath.moveTo(x, yLow)
                            started = true
                        } else {
                            upperPath.lineTo(x, yUp)
                            lowerPath.lineTo(x, yLow)
                        }
                    }
                }

                if (started) {
                    drawPath(
                        path = upperPath,
                        color = AccentPurple.copy(alpha = 0.7f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    drawPath(
                        path = lowerPath,
                        color = AccentPurple.copy(alpha = 0.7f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }

            // 3. Draw EMA Fast (Cyan) and EMA Slow (Gold)
            if (settings.showEma) {
                val fastPath = Path()
                val slowPath = Path()
                var fastStarted = false
                var slowStarted = false

                visibleCandles.forEachIndexed { i, c ->
                    val x = i * candleWidth + (candleWidth / 2f)
                    c.emaShort?.let { fast ->
                        val y = priceToY(fast)
                        if (!fastStarted) {
                            fastPath.moveTo(x, y)
                            fastStarted = true
                        } else {
                            fastPath.lineTo(x, y)
                        }
                    }
                    c.emaLong?.let { slow ->
                        val y = priceToY(slow)
                        if (!slowStarted) {
                            slowPath.moveTo(x, y)
                            slowStarted = true
                        } else {
                            slowPath.lineTo(x, y)
                        }
                    }
                }

                if (fastStarted) {
                    drawPath(
                        path = fastPath,
                        color = AccentCyan,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
                if (slowStarted) {
                    drawPath(
                        path = slowPath,
                        color = AccentGold,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
            }

            // 4. Draw Candlesticks & Volumes
            visibleCandles.forEachIndexed { i, candle ->
                val xCenter = i * candleWidth + (candleWidth / 2f)
                val isBullish = candle.close >= candle.open
                val candleColor = if (isBullish) BullGreen else BearRed

                // Volume Bar
                if (settings.showVolume) {
                    val volRatio = (candle.volume / maxVolume).toFloat().coerceIn(0.05f, 1f)
                    val volBarHeight = volRatio * volumeHeight
                    val volY = canvasHeight - volBarHeight
                    drawRect(
                        color = candleColor.copy(alpha = 0.35f),
                        topLeft = Offset(xCenter - (bodyWidth / 2f), volY),
                        size = Size(bodyWidth, volBarHeight)
                    )
                }

                // Candle Wicks (High to Low)
                val yHigh = priceToY(candle.high)
                val yLow = priceToY(candle.low)
                drawLine(
                    color = candleColor,
                    start = Offset(xCenter, yHigh),
                    end = Offset(xCenter, yLow),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Candle Body (Open to Close)
                val yOpen = priceToY(candle.open)
                val yClose = priceToY(candle.close)
                val bodyTop = min(yOpen, yClose)
                val bodyHeight = max(kotlin.math.abs(yClose - yOpen), 2.5f)

                drawRect(
                    color = candleColor,
                    topLeft = Offset(xCenter - (bodyWidth / 2f), bodyTop),
                    size = Size(bodyWidth, bodyHeight)
                )

                // 5. Punchman TradingView Buy & Sell Badges/Markers
                if (settings.showBuySellMarkers && candle.signal != null && candle.signal != SignalType.NEUTRAL) {
                    val isBuy = candle.signal == SignalType.BUY || candle.signal == SignalType.STRONG_BUY
                    val isStrong = candle.signal == SignalType.STRONG_BUY || candle.signal == SignalType.STRONG_SELL

                    val markerY = if (isBuy) yLow + 20f else yHigh - 24f
                    val badgeColor = if (isBuy) BullGreenBright else BearRedBright
                    val badgeText = when (candle.signal) {
                        SignalType.STRONG_BUY -> "STRONG BUY"
                        SignalType.BUY -> "BUY"
                        SignalType.STRONG_SELL -> "STRONG SELL"
                        SignalType.SELL -> "SELL"
                        else -> ""
                    }

                    // Draw Triangle marker
                    val arrowPath = Path()
                    if (isBuy) {
                        arrowPath.moveTo(xCenter, yLow + 4f)
                        arrowPath.lineTo(xCenter - 6f, yLow + 14f)
                        arrowPath.lineTo(xCenter + 6f, yLow + 14f)
                    } else {
                        arrowPath.moveTo(xCenter, yHigh - 4f)
                        arrowPath.lineTo(xCenter - 6f, yHigh - 14f)
                        arrowPath.lineTo(xCenter + 6f, yHigh - 14f)
                    }
                    arrowPath.close()
                    drawPath(path = arrowPath, color = badgeColor)

                    // Draw Mini text label for Strong signals
                    if (isStrong) {
                        val labelLayout = textMeasurer.measure(
                            text = badgeText,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        val pillWidth = labelLayout.size.width + 12f
                        val pillHeight = labelLayout.size.height + 6f
                        val pillX = (xCenter - (pillWidth / 2f)).coerceIn(4f, canvasWidth - pillWidth - 4f)
                        val pillY = if (isBuy) yLow + 16f else yHigh - 16f - pillHeight

                        drawRoundRect(
                            color = if (isBuy) BullGreen.copy(alpha = 0.95f) else BearRed.copy(alpha = 0.95f),
                            topLeft = Offset(pillX, pillY),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = badgeText,
                            topLeft = Offset(pillX + 6f, pillY + 3f),
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // 6. Draw Algo Target Zones (Stop Loss & Take Profit dashed lines)
                    if (settings.showTargetZones && i == visibleCandles.lastIndex) {
                        candle.takeProfit1?.let { tp1 ->
                            val tpY = priceToY(tp1)
                            drawLine(
                                color = BullGreenBright,
                                start = Offset(0f, tpY),
                                end = Offset(canvasWidth, tpY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            )
                        }
                        candle.stopLoss?.let { sl ->
                            val slY = priceToY(sl)
                            drawLine(
                                color = BearRedBright,
                                start = Offset(0f, slY),
                                end = Offset(canvasWidth, slY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            )
                        }
                    }
                }
            }

            // 7. Interactive Crosshair when inspecting candles
            selectedIndex?.let { selIdx ->
                val offsetFromEnd = candles.size - 1 - selIdx
                if (offsetFromEnd in 0 until visibleCount) {
                    val localIdx = visibleCount - 1 - offsetFromEnd
                    val candle = visibleCandles[localIdx]
                    val x = localIdx * candleWidth + (candleWidth / 2f)
                    val y = priceToY(candle.close)

                    // Vertical dashed crosshair
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(x, 0f),
                        end = Offset(x, canvasHeight),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )

                    // Horizontal dashed crosshair
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )

                    // Floating Price Bubble on Right Axis
                    val priceStr = formatDynamicPrice(candle.close)
                    val bubbleLayout = textMeasurer.measure(
                        text = priceStr,
                        style = TextStyle(color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    val bubbleW = bubbleLayout.size.width + 16f
                    val bubbleH = bubbleLayout.size.height + 8f
                    val bubbleY = (y - (bubbleH / 2f)).coerceIn(0f, canvasHeight - bubbleH)

                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(canvasWidth - bubbleW - 4f, bubbleY),
                        size = Size(bubbleW, bubbleH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = priceStr,
                        topLeft = Offset(canvasWidth - bubbleW + 4f, bubbleY + 4f),
                        style = TextStyle(color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

fun formatDynamicPrice(price: Double): String {
    return when {
        price >= 1000.0 -> String.format("%,.2f", price)
        price >= 1.0 -> String.format("%.3f", price)
        price >= 0.001 -> String.format("%.5f", price)
        else -> String.format("%.7f", price)
    }
}
