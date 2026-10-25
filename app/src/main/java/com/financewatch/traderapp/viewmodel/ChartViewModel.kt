package com.financewatch.traderapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.financewatch.traderapp.data.remote.model.Article
import com.financewatch.traderapp.repository.ChartRepository
import com.financewatch.traderapp.repository.TradingNewsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ──────────────────────────────────────────────────────────────────────────────
// State UI — Chart
// ──────────────────────────────────────────────────────────────────────────────

sealed class ChartUiState {
    object Loading : ChartUiState()
    data class Success(val data: com.financewatch.traderapp.data.remote.model.ChartData) : ChartUiState()
    data class Error(val message: String) : ChartUiState()
}

// ──────────────────────────────────────────────────────────────────────────────
// State UI — Trading News
// ──────────────────────────────────────────────────────────────────────────────

sealed class TradingNewsUiState {
    object Loading : TradingNewsUiState()
    data class Success(
        val articles: List<Article>,
        val categoryLabel: String
    ) : TradingNewsUiState()
    object Empty : TradingNewsUiState()
    object Error : TradingNewsUiState()
}

// ──────────────────────────────────────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel pentru tabul Piete.
 * Gestioneaza:
 *  - Chart live (Yahoo Finance) → [chartState]
 *  - Stiri de trading per pereche (NewsAPI) → [tradingNewsState]
 *
 * Ambele sunt incarcate in PARALEL cand se schimba simbolul selectat.
 */
class ChartViewModel(
    private val chartRepo: ChartRepository,
    private val newsRepo: TradingNewsRepository
) : ViewModel() {

    // ── Chart ──────────────────────────────────────────────────────────────────

    private val _chartState = MutableStateFlow<ChartUiState>(ChartUiState.Loading)
    val chartState: StateFlow<ChartUiState> = _chartState

    // ── Trading News ───────────────────────────────────────────────────────────

    private val _tradingNewsState = MutableStateFlow<TradingNewsUiState>(TradingNewsUiState.Loading)
    val tradingNewsState: StateFlow<TradingNewsUiState> = _tradingNewsState

    // ── Simboluri disponibile ──────────────────────────────────────────────────

    val symbols = listOf("BTC-USD", "ETH-USD", "EURUSD=X", "GC=F", "^NDX")

    private var currentSymbol = symbols.first()

    init {
        loadChart(currentSymbol)
    }

    /**
     * Incarca chart + stiri in PARALEL pentru simbolul dat.
     * Foloseste async/await pentru a minimiza latenta totala.
     */
    fun loadChart(symbol: String) {
        currentSymbol = symbol
        _chartState.value       = ChartUiState.Loading
        _tradingNewsState.value = TradingNewsUiState.Loading

        viewModelScope.launch {
            // Porneste ambele request-uri simultan
            val chartDeferred = async { chartRepo.fetchChart(symbol) }
            val newsDeferred  = async { newsRepo.fetchTradingNews(symbol) }

            // Actualizeaza chart state
            val chartData = chartDeferred.await()
            _chartState.value = if (chartData != null) {
                ChartUiState.Success(chartData)
            } else {
                ChartUiState.Error("Nu s-au putut incarca datele pentru $symbol")
            }

            // Actualizeaza news state
            val result = newsDeferred.await()
            val label  = newsRepo.getCategoryLabel(symbol)
            val articles = result.getOrNull()
            _tradingNewsState.value = when {
                articles == null   -> TradingNewsUiState.Error
                articles.isEmpty() -> TradingNewsUiState.Empty
                else               -> TradingNewsUiState.Success(articles, label)
            }
        }
    }

    // ── Factory ────────────────────────────────────────────────────────────────

    class Factory(
        private val chartRepo: ChartRepository,
        private val newsRepo: TradingNewsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChartViewModel::class.java))
                return ChartViewModel(chartRepo, newsRepo) as T
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
