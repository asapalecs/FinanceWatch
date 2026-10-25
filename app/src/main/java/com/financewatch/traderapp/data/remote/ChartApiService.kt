package com.financewatch.traderapp.data.remote

import com.financewatch.traderapp.data.remote.model.YahooChartResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interfata Retrofit pentru Yahoo Finance v8 chart API.
 * Base URL: https://query1.finance.yahoo.com/
 * Nu necesita API key.
 *
 * Simboluri suportate:
 *  - BTC-USD  (Bitcoin)
 *  - ETH-USD  (Ethereum)
 *  - EURUSD=X (EUR/USD Forex)
 *  - GC=F     (Gold Futures)
 *  - ^NDX     (Nasdaq 100)
 */
interface ChartApiService {

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "7d"
    ): Response<YahooChartResponse>
}
