package com.financewatch.traderapp.data.remote.model

/**
 * Modele Gson pentru raspunsul Yahoo Finance v8 chart API.
 * Endpoint: https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=1d&range=7d
 */
data class YahooChartResponse(
    val chart: ChartContainer?
)

data class ChartContainer(
    val result: List<ChartResult>?,
    val error: YahooError?
)

data class YahooError(
    val code: String?,
    val description: String?
)

data class ChartResult(
    val meta: ChartMeta,
    val timestamp: List<Long>?,
    val indicators: ChartIndicators?
)

data class ChartMeta(
    val regularMarketPrice: Double = 0.0,
    val chartPreviousClose: Double = 0.0,
    val currency: String? = "USD",
    val symbol: String? = null
)

data class ChartIndicators(
    val quote: List<Quote>?
)

data class Quote(
    val close: List<Double?>?
)

// ─── Model normalizat folosit in UI ───────────────────────────────────────────

data class ChartData(
    val symbol: String,
    val displayName: String,
    val emoji: String,
    val currentPrice: Double,
    val currency: String,
    val changePercent: Double,       // % fata de inchiderea anterioara
    val pricePoints: List<Float>     // seria de preturi pentru chart (curatata de null)
) {
    val isPositive: Boolean get() = changePercent >= 0
    val changeSign:  String get() = if (isPositive) "+" else ""
}
