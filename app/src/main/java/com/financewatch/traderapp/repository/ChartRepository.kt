package com.financewatch.traderapp.repository

import com.financewatch.traderapp.data.remote.ChartApiService
import com.financewatch.traderapp.data.remote.model.ChartData

/**
 * Repository pentru date de chart din Yahoo Finance.
 * Normalizeaza raspunsul brut intr-un ChartData curat pentru UI.
 *
 * Simboluri disponibile:
 *  Symbol     | Afisaj       | Emoji
 *  -----------|--------------|------
 *  BTC-USD    | Bitcoin      | ₿
 *  ETH-USD    | Ethereum     | Ξ
 *  EURUSD=X   | EUR/USD      | 💱
 *  GC=F       | Gold         | 🥇
 *  ^NDX       | Nasdaq 100   | 📈
 */
class ChartRepository(private val api: ChartApiService) {

    /** Metadate afisare pentru fiecare simbol */
    private val symbolMeta = mapOf(
        "BTC-USD"  to Triple("Bitcoin",    "₿",  "USD"),
        "ETH-USD"  to Triple("Ethereum",   "Ξ",  "USD"),
        "EURUSD=X" to Triple("EUR/USD",    "💱", "USD"),
        "GC=F"     to Triple("Gold",       "🥇", "USD"),
        "^NDX"     to Triple("Nasdaq 100", "📈", "USD")
    )

    /**
     * Incarca datele de chart pentru un simbol.
     * @return ChartData normalizat sau null in caz de eroare
     */
    suspend fun fetchChart(symbol: String): ChartData? {
        return try {
            val response = api.getChart(symbol)
            if (!response.isSuccessful) return null

            val result = response.body()?.chart?.result?.firstOrNull() ?: return null
            val meta   = result.meta
            val closes = result.indicators?.quote?.firstOrNull()?.close ?: return null

            // Filtreaza null-urile si converteste la Float
            val pricePoints = closes.filterNotNull().map { it.toFloat() }
            if (pricePoints.isEmpty()) return null

            val previousClose = meta.chartPreviousClose.takeIf { it > 0.0 } ?: pricePoints.first().toDouble()
            val currentPrice  = meta.regularMarketPrice.takeIf { it > 0.0 } ?: pricePoints.last().toDouble()
            val changePercent = if (previousClose > 0.0) {
                ((currentPrice - previousClose) / previousClose) * 100.0
            } else 0.0

            val (displayName, emoji, _) = symbolMeta[symbol]
                ?: Triple(meta.symbol ?: symbol, "📊", "USD")
            val currency = meta.currency ?: "USD"

            ChartData(
                symbol       = symbol,
                displayName  = displayName,
                emoji        = emoji,
                currentPrice = currentPrice,
                currency     = currency,
                changePercent = changePercent,
                pricePoints  = pricePoints
            )
        } catch (e: Exception) {
            null
        }
    }
}
