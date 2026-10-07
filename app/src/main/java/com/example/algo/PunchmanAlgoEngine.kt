package com.example.algo

import com.example.model.AlgoSettings
import com.example.model.Candle
import com.example.model.SignalType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object PunchmanAlgoEngine {

    /**
     * Computes technical indicators (EMA Fast, EMA Slow, RSI, Bollinger Bands, ATR)
     * and evaluates TradingView-style algorithmic signals (Buy, Strong Buy, Sell, Strong Sell).
     */
    fun processCandles(rawCandles: List<Candle>, settings: AlgoSettings): List<Candle> {
        if (rawCandles.isEmpty()) return emptyList()

        val n = rawCandles.size
        val closes = rawCandles.map { it.close }

        // 1. Calculate EMAs
        val emaFast = calculateEMA(closes, settings.emaFastPeriod)
        val emaSlow = calculateEMA(closes, settings.emaSlowPeriod)

        // 2. Calculate RSI
        val rsiList = calculateRSI(closes, settings.rsiPeriod)

        // 3. Calculate Bollinger Bands (20, 2)
        val (upperBands, lowerBands) = calculateBollingerBands(closes, 20, 2.0)

        // 4. Calculate ATR (Average True Range) for dynamic Stop Loss & Take Profit
        val atrs = calculateATR(rawCandles, 14)

        // Combine into annotated candles
        val enriched = ArrayList<Candle>(n)
        for (i in 0 until n) {
            val candle = rawCandles[i]
            val fast = emaFast[i]
            val slow = emaSlow[i]
            val rsi = rsiList[i]
            val upper = upperBands[i]
            val lower = lowerBands[i]
            val atr = atrs[i] ?: (candle.close * 0.015) // default 1.5% if ATR not ready

            var signal: SignalType? = null
            var reason: String? = null
            var sl: Double? = null
            var tp1: Double? = null
            var tp2: Double? = null

            // Generate signal if we have enough lookback
            if (i >= settings.emaSlowPeriod && fast != null && slow != null && rsi != null) {
                val prevFast = emaFast[i - 1]
                val prevSlow = emaSlow[i - 1]
                val prevClose = rawCandles[i - 1].close

                val bullishCross = prevFast != null && prevSlow != null && prevFast <= prevSlow && fast > slow
                val bearishCross = prevFast != null && prevSlow != null && prevFast >= prevSlow && fast < slow

                // Punchman Multi-Condition Algorithmic Logic:
                // BUY condition: Fast EMA crosses above Slow EMA, or Oversold RSI reversal bouncing off lower BB
                val rsiOversold = rsi < settings.rsiOversold
                val rsiOverbought = rsi > settings.rsiOverbought
                val rsiBouncingUp = i > 1 && rsiList[i - 1] != null && rsiList[i - 1]!! < 35 && rsi > rsiList[i - 1]!!
                val rsiDroppingDown = i > 1 && rsiList[i - 1] != null && rsiList[i - 1]!! > 65 && rsi < rsiList[i - 1]!!

                val priceAboveFast = candle.close > fast
                val priceBelowFast = candle.close < fast

                if (bullishCross && priceAboveFast) {
                    if (rsi in 45.0..65.0) {
                        signal = SignalType.STRONG_BUY
                        reason = "Golden EMA Cross (${settings.emaFastPeriod}/${settings.emaSlowPeriod}) + Momentum RSI (${String.format("%.1f", rsi)})"
                    } else {
                        signal = SignalType.BUY
                        reason = "Bullish EMA (${settings.emaFastPeriod}) Breakout"
                    }
                } else if (bearishCross && priceBelowFast) {
                    if (rsi in 35.0..55.0) {
                        signal = SignalType.STRONG_SELL
                        reason = "Death EMA Cross (${settings.emaFastPeriod}/${settings.emaSlowPeriod}) + Momentum Breakdown (${String.format("%.1f", rsi)})"
                    } else {
                        signal = SignalType.SELL
                        reason = "Bearish EMA (${settings.emaFastPeriod}) Rejection"
                    }
                } else if (lower != null && candle.low <= lower && candle.close > lower && rsiBouncingUp) {
                    signal = SignalType.BUY
                    reason = "Mean Reversion: Lower Bollinger Band Bounce + RSI Bull Hook"
                } else if (upper != null && candle.high >= upper && candle.close < upper && rsiDroppingDown) {
                    signal = SignalType.SELL
                    reason = "Mean Reversion: Upper Bollinger Band Rejection + RSI Bear Hook"
                }

                // If signal was generated, calculate risk management target zones
                if (signal == SignalType.BUY || signal == SignalType.STRONG_BUY) {
                    val multiplier = if (signal == SignalType.STRONG_BUY) 1.2 else 1.5
                    sl = candle.close - (atr * multiplier)
                    tp1 = candle.close + (atr * multiplier * settings.riskRewardRatio * 0.75)
                    tp2 = candle.close + (atr * multiplier * settings.riskRewardRatio * 1.5)
                } else if (signal == SignalType.SELL || signal == SignalType.STRONG_SELL) {
                    val multiplier = if (signal == SignalType.STRONG_SELL) 1.2 else 1.5
                    sl = candle.close + (atr * multiplier)
                    tp1 = candle.close - (atr * multiplier * settings.riskRewardRatio * 0.75)
                    tp2 = candle.close - (atr * multiplier * settings.riskRewardRatio * 1.5)
                }
            }

            enriched.add(
                candle.copy(
                    emaShort = fast,
                    emaLong = slow,
                    rsi = rsi,
                    upperBand = upper,
                    lowerBand = lower,
                    signal = signal,
                    signalReason = reason,
                    stopLoss = sl,
                    takeProfit1 = tp1,
                    takeProfit2 = tp2
                )
            )
        }

        return enriched
    }

    private fun calculateEMA(data: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(data.size) { null }
        if (data.size < period) return result

        val multiplier = 2.0 / (period + 1.0)
        // First EMA is simple SMA
        var sum = 0.0
        for (i in 0 until period) {
            sum += data[i]
        }
        var ema = sum / period
        result[period - 1] = ema

        for (i in period until data.size) {
            ema = (data[i] - ema) * multiplier + ema
            result[i] = ema
        }
        return result
    }

    private fun calculateRSI(data: List<Double>, period: Int = 14): List<Double?> {
        val result = MutableList<Double?>(data.size) { null }
        if (data.size <= period) return result

        var gains = 0.0
        var losses = 0.0

        for (i in 1..period) {
            val change = data[i] - data[i - 1]
            if (change > 0) gains += change else losses += -change
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        var rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        result[period] = 100.0 - (100.0 / (1.0 + rs))

        for (i in (period + 1) until data.size) {
            val change = data[i] - data[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) -change else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            result[i] = 100.0 - (100.0 / (1.0 + rs))
        }

        return result
    }

    private fun calculateBollingerBands(
        data: List<Double>,
        period: Int = 20,
        stdDevMultiplier: Double = 2.0
    ): Pair<List<Double?>, List<Double?>> {
        val uppers = MutableList<Double?>(data.size) { null }
        val lowers = MutableList<Double?>(data.size) { null }
        if (data.size < period) return Pair(uppers, lowers)

        for (i in (period - 1) until data.size) {
            val slice = data.subList(i - period + 1, i + 1)
            val mean = slice.average()
            val variance = slice.map { (it - mean) * (it - mean) }.average()
            val stdDev = sqrt(variance)

            uppers[i] = mean + (stdDevMultiplier * stdDev)
            lowers[i] = mean - (stdDevMultiplier * stdDev)
        }

        return Pair(uppers, lowers)
    }

    private fun calculateATR(candles: List<Candle>, period: Int = 14): List<Double?> {
        val atrs = MutableList<Double?>(candles.size) { null }
        if (candles.size < period + 1) return atrs

        val trList = mutableListOf<Double>()
        for (i in candles.indices) {
            if (i == 0) {
                trList.add(candles[i].high - candles[i].low)
            } else {
                val hl = candles[i].high - candles[i].low
                val hc = kotlin.math.abs(candles[i].high - candles[i - 1].close)
                val lc = kotlin.math.abs(candles[i].low - candles[i - 1].close)
                trList.add(max(hl, max(hc, lc)))
            }
        }

        var atr = trList.subList(1, period + 1).average()
        atrs[period] = atr

        for (i in (period + 1) until candles.size) {
            atr = (atr * (period - 1) + trList[i]) / period
            atrs[i] = atr
        }

        return atrs
    }
}
