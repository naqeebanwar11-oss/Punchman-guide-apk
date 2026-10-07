package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.repository.TradingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppNavTab {
    CHART,
    SIGNALS,
    MARKETS,
    PAPER_TRADING,
    ALGO_SETTINGS
}

data class TradingUiState(
    val selectedPair: MarketPair = MarketPair(
        symbol = "BTCUSDT",
        displayName = "BTC/USDT",
        assetName = "Bitcoin",
        price = 67420.0,
        changePercent24h = 2.45,
        high24h = 68500.0,
        low24h = 66200.0,
        volume24h = 428190000.0,
        currentSignal = SignalType.STRONG_BUY,
        signalConfidence = 91
    ),
    val marketPairs: List<MarketPair> = emptyList(),
    val candles: List<Candle> = emptyList(),
    val signals: List<AlgoSignal> = emptyList(),
    val paperTrades: List<PaperTrade> = emptyList(),
    val selectedTimeframe: Timeframe = Timeframe.M15,
    val algoSettings: AlgoSettings = AlgoSettings(),
    val currentTab: AppNavTab = AppNavTab.CHART,
    val isLoadingCandles: Boolean = false,
    val isLoadingMarkets: Boolean = false,
    val isRealtimeStreaming: Boolean = true,
    val paperBalance: Double = 10000.0, // Virtual starting balance
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class TradingViewModel(
    private val repository: TradingRepository = TradingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TradingUiState())
    val uiState: StateFlow<TradingUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        loadMarketPairs()
        loadCandlesForSelectedPair()
        startLivePolling()
    }

    fun selectTab(tab: AppNavTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun selectPair(pair: MarketPair) {
        _uiState.update { it.copy(selectedPair = pair) }
        loadCandlesForSelectedPair()
    }

    fun selectTimeframe(tf: Timeframe) {
        _uiState.update { it.copy(selectedTimeframe = tf) }
        loadCandlesForSelectedPair()
    }

    fun updateAlgoSettings(settings: AlgoSettings) {
        _uiState.update { it.copy(algoSettings = settings) }
        loadCandlesForSelectedPair()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun loadMarketPairs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMarkets = true) }
            val pairs = repository.fetchMarketPairs()
            _uiState.update { current ->
                val updatedSelected = pairs.find { it.symbol == current.selectedPair.symbol } ?: current.selectedPair
                current.copy(
                    marketPairs = pairs,
                    selectedPair = updatedSelected,
                    isLoadingMarkets = false
                )
            }
        }
    }

    fun loadCandlesForSelectedPair() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCandles = true) }
            val currentSymbol = _uiState.value.selectedPair.symbol
            val timeframe = _uiState.value.selectedTimeframe
            val settings = _uiState.value.algoSettings

            val processedCandles = repository.fetchKlines(
                symbol = currentSymbol,
                timeframe = timeframe,
                settings = settings
            )

            val generatedSignals = repository.extractSignalsFromCandles(
                symbol = currentSymbol,
                timeframe = timeframe,
                processedCandles = processedCandles
            )

            // Update current pair's latest price & overall signal
            val latestClose = processedCandles.lastOrNull()?.close
            val latestSignal = processedCandles.lastOrNull { it.signal != null && it.signal != SignalType.NEUTRAL }?.signal
                ?: SignalType.NEUTRAL

            _uiState.update { state ->
                val updatedPair = if (latestClose != null) {
                    state.selectedPair.copy(
                        price = latestClose,
                        currentSignal = latestSignal
                    )
                } else state.selectedPair

                state.copy(
                    candles = processedCandles,
                    signals = generatedSignals,
                    selectedPair = updatedPair,
                    isLoadingCandles = false
                )
            }
        }
    }

    private fun startLivePolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(4000) // 4-second refresh ticker for real-time market data
                if (_uiState.value.isRealtimeStreaming) {
                    val pairs = repository.fetchMarketPairs()
                    val currentSym = _uiState.value.selectedPair.symbol
                    val updatedPair = pairs.find { it.symbol == currentSym }

                    _uiState.update { state ->
                        // Re-evaluate open paper trades against latest price
                        val updatedTrades = if (updatedPair != null) {
                            state.paperTrades.map { trade ->
                                if (trade.status == TradeStatus.OPEN && trade.symbol == currentSym) {
                                    val isBuy = trade.type == SignalType.BUY || trade.type == SignalType.STRONG_BUY
                                    val priceDiff = if (isBuy) updatedPair.price - trade.entryPrice else trade.entryPrice - updatedPair.price
                                    val pnl = priceDiff * trade.quantity
                                    val pnlPercent = (priceDiff / trade.entryPrice) * 100.0

                                    val newStatus = when {
                                        isBuy && updatedPair.price >= trade.target1 -> TradeStatus.CLOSED_PROFIT
                                        isBuy && updatedPair.price <= trade.stopLoss -> TradeStatus.CLOSED_LOSS
                                        !isBuy && updatedPair.price <= trade.target1 -> TradeStatus.CLOSED_PROFIT
                                        !isBuy && updatedPair.price >= trade.stopLoss -> TradeStatus.CLOSED_LOSS
                                        else -> TradeStatus.OPEN
                                    }

                                    trade.copy(
                                        pnl = pnl,
                                        pnlPercent = pnlPercent,
                                        status = newStatus,
                                        exitPrice = if (newStatus != TradeStatus.OPEN) updatedPair.price else null,
                                        exitTime = if (newStatus != TradeStatus.OPEN) System.currentTimeMillis() else null
                                    )
                                } else trade
                            }
                        } else state.paperTrades

                        state.copy(
                            marketPairs = pairs,
                            selectedPair = updatedPair ?: state.selectedPair,
                            paperTrades = updatedTrades
                        )
                    }
                }
            }
        }
    }

    fun executePaperTrade(signal: AlgoSignal) {
        val currentPrice = signal.entryPrice
        val investmentAmount = 500.0 // $500 per simulated paper trade
        val quantity = investmentAmount / currentPrice

        val newTrade = PaperTrade(
            id = UUID.randomUUID().toString(),
            symbol = signal.symbol,
            type = signal.type,
            entryPrice = currentPrice,
            quantity = quantity,
            entryTime = System.currentTimeMillis(),
            stopLoss = signal.stopLoss,
            target1 = signal.target1,
            target2 = signal.target2,
            status = TradeStatus.OPEN,
            pnl = 0.0,
            pnlPercent = 0.0
        )

        _uiState.update {
            it.copy(
                paperTrades = listOf(newTrade) + it.paperTrades,
                paperBalance = it.paperBalance - investmentAmount
            )
        }
    }

    fun closeTradeManually(tradeId: String) {
        _uiState.update { state ->
            val updated = state.paperTrades.map { trade ->
                if (trade.id == tradeId && trade.status == TradeStatus.OPEN) {
                    val currentPrice = state.selectedPair.price
                    val isBuy = trade.type == SignalType.BUY || trade.type == SignalType.STRONG_BUY
                    val priceDiff = if (isBuy) currentPrice - trade.entryPrice else trade.entryPrice - currentPrice
                    val pnl = priceDiff * trade.quantity
                    val pnlPercent = (priceDiff / trade.entryPrice) * 100.0
                    val finalStatus = if (pnl >= 0) TradeStatus.CLOSED_PROFIT else TradeStatus.CLOSED_LOSS

                    trade.copy(
                        status = finalStatus,
                        exitPrice = currentPrice,
                        exitTime = System.currentTimeMillis(),
                        pnl = pnl,
                        pnlPercent = pnlPercent
                    )
                } else trade
            }

            val tradeClosed = state.paperTrades.find { it.id == tradeId }
            val refund = if (tradeClosed != null) (tradeClosed.quantity * tradeClosed.entryPrice) + tradeClosed.pnl else 0.0

            state.copy(
                paperTrades = updated,
                paperBalance = state.paperBalance + refund
            )
        }
    }

    fun resetPaperAccount() {
        _uiState.update {
            it.copy(
                paperTrades = emptyList(),
                paperBalance = 10000.0
            )
        }
    }
}
