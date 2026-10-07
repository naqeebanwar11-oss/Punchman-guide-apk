package com.example

import com.example.algo.PunchmanAlgoEngine
import com.example.model.AlgoSettings
import com.example.model.Candle
import com.example.model.SignalType
import org.junit.Assert.*
import org.junit.Test

class PunchmanAlgoUnitTest {

    @Test
    fun testPunchmanAlgoEngineCalculatesEmaAndRsi() {
        val candles = mutableListOf<Candle>()
        var price = 100.0

        for (i in 0 until 50) {
            price += if (i % 2 == 0) 1.5 else -0.5
            candles.add(
                Candle(
                    timestamp = 1000L * i,
                    open = price - 0.5,
                    high = price + 1.0,
                    low = price - 1.0,
                    close = price,
                    volume = 100.0
                )
            )
        }

        val settings = AlgoSettings(
            emaFastPeriod = 9,
            emaSlowPeriod = 21,
            rsiPeriod = 14
        )

        val processed = PunchmanAlgoEngine.processCandles(candles, settings)
        assertEquals(50, processed.size)

        // After lookback period, EMA and RSI should be non-null
        val lastCandle = processed.last()
        assertNotNull(lastCandle.emaShort)
        assertNotNull(lastCandle.emaLong)
        assertNotNull(lastCandle.rsi)
        assertTrue(lastCandle.rsi!! in 0.0..100.0)
    }

    @Test
    fun testRiskManagementTargetZonesGeneratedForSignals() {
        val candles = mutableListOf<Candle>()
        var price = 50.0

        // Create a strong upwards momentum trend
        for (i in 0 until 40) {
            price += 2.0
            candles.add(
                Candle(
                    timestamp = 1000L * i,
                    open = price - 1.0,
                    high = price + 2.0,
                    low = price - 1.0,
                    close = price,
                    volume = 500.0
                )
            )
        }

        val settings = AlgoSettings()
        val processed = PunchmanAlgoEngine.processCandles(candles, settings)
        val signalCandle = processed.find { it.signal == SignalType.BUY || it.signal == SignalType.STRONG_BUY }

        if (signalCandle != null) {
            assertNotNull(signalCandle.stopLoss)
            assertNotNull(signalCandle.takeProfit1)
            assertNotNull(signalCandle.takeProfit2)
            // For Buy signal, Take Profit must be higher than entry and Stop Loss lower
            assertTrue(signalCandle.takeProfit1!! > signalCandle.close)
            assertTrue(signalCandle.stopLoss!! < signalCandle.close)
        }
    }
}
