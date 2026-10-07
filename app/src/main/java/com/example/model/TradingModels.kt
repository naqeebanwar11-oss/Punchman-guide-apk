package com.example.model

enum class SignalType {
    STRONG_BUY,
    BUY,
    NEUTRAL,
    SELL,
    STRONG_SELL
}

enum class Timeframe(val label: String, val apiInterval: String, val candleMinutes: Int) {
    M1("1m", "1m", 1),
    M5("5m", "5m", 5),
    M15("15m", "15m", 15),
    H1("1h", "1h", 60),
    H4("4h", "4h", 240),
    D1("1D", "1d", 1440)
}

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    // Indicators calculated by Punchman Algo engine
    val emaShort: Double? = null,
    val emaLong: Double? = null,
    val rsi: Double? = null,
    val upperBand: Double? = null,
    val lowerBand: Double? = null,
    val signal: SignalType? = null,
    val signalReason: String? = null,
    val stopLoss: Double? = null,
    val takeProfit1: Double? = null,
    val takeProfit2: Double? = null
)

data class MarketPair(
    val symbol: String, // e.g. "BTCUSDT"
    val displayName: String, // e.g. "BTC/USDT"
    val assetName: String, // e.g. "Bitcoin"
    val price: Double,
    val changePercent24h: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val currentSignal: SignalType = SignalType.NEUTRAL,
    val signalConfidence: Int = 75,
    val category: String = "Crypto"
)

data class AlgoSignal(
    val id: String,
    val symbol: String,
    val type: SignalType,
    val entryPrice: Double,
    val currentPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val confidence: Int,
    val timeAgo: String,
    val reasoning: String,
    val status: String = "Active", // Active, Target Hit, Stopped Out
    val rsiValue: Double,
    val emaCrossStatus: String,
    val timeframe: Timeframe = Timeframe.M15
)

data class PaperTrade(
    val id: String,
    val symbol: String,
    val type: SignalType,
    val entryPrice: Double,
    val quantity: Double,
    val entryTime: Long,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val status: TradeStatus = TradeStatus.OPEN,
    val exitPrice: Double? = null,
    val exitTime: Long? = null,
    val pnl: Double = 0.0,
    val pnlPercent: Double = 0.0
)

enum class TradeStatus {
    OPEN,
    CLOSED_PROFIT,
    CLOSED_LOSS
}

data class AlgoSettings(
    val emaFastPeriod: Int = 9,
    val emaSlowPeriod: Int = 21,
    val rsiPeriod: Int = 14,
    val rsiOversold: Double = 30.0,
    val rsiOverbought: Double = 70.0,
    val riskRewardRatio: Double = 2.0,
    val showEma: Boolean = true,
    val showBollingerBands: Boolean = true,
    val showVolume: Boolean = true,
    val showBuySellMarkers: Boolean = true,
    val showTargetZones: Boolean = true,
    val sensitivity: String = "Balanced" // Aggressive, Balanced, Conservative
)
