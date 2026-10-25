package com.financewatch.traderapp.repository

import android.util.Log
import com.financewatch.traderapp.data.remote.NewsApiService
import com.financewatch.traderapp.data.remote.model.Article

/**
 * Repository pentru stiri de trading specifice fiecarei perechi.
 *
 * Strategia: query-uri scurte si simple → mai fiabile pe NewsAPI free plan.
 * Fiecare simbol are un query primar si unul de fallback.
 * Erorile sunt propagate (nu inghitite) pentru afisare in UI.
 */
class TradingNewsRepository(private val api: NewsApiService) {

    companion object {
        private const val TAG = "TradingNewsRepo"
    }

    /** Query primar per simbol — scurt, fara operatori boolean complecsi */
    private val primaryQueries = mapOf(
        "BTC-USD"  to "bitcoin trading",
        "ETH-USD"  to "ethereum trading",
        "EURUSD=X" to "EURUSD forex",
        "GC=F"     to "gold XAU price",
        "^NDX"     to "nasdaq 100 stocks"
    )

    /** Query de fallback daca cel primar returneaza 0 rezultate */
    private val fallbackQueries = mapOf(
        "BTC-USD"  to "bitcoin",
        "ETH-USD"  to "ethereum",
        "EURUSD=X" to "euro dollar exchange rate",
        "GC=F"     to "gold price market",
        "^NDX"     to "nasdaq technology stocks"
    )

    /** Label categorie per simbol */
    private val categoryLabels = mapOf(
        "BTC-USD"  to "CRYPTO",
        "ETH-USD"  to "CRYPTO",
        "EURUSD=X" to "FOREX",
        "GC=F"     to "GOLD",
        "^NDX"     to "NASDAQ"
    )

    /**
     * Incarca stiri de trading pentru un simbol.
     * @return Result cu lista de articole (max 10) sau eroare cu mesaj descriptiv
     */
    suspend fun fetchTradingNews(symbol: String): Result<List<Article>> {
        val primaryQuery = primaryQueries[symbol]
            ?: return Result.failure(Exception("Simbol necunoscut: $symbol"))

        // Incearca query primar
        val primaryResult = doFetch(primaryQuery, symbol)
        if (primaryResult.isSuccess) {
            val articles = primaryResult.getOrNull() ?: emptyList()
            if (articles.isNotEmpty()) {
                Log.d(TAG, "[$symbol] query primar OK → ${articles.size} articole")
                return Result.success(articles)
            }
            Log.w(TAG, "[$symbol] query primar a returnat 0 rezultate, incerc fallback...")
        } else {
            Log.e(TAG, "[$symbol] query primar esuat: ${primaryResult.exceptionOrNull()?.message}")
        }

        // Fallback
        val fallbackQuery = fallbackQueries[symbol] ?: return primaryResult
        val fallbackResult = doFetch(fallbackQuery, symbol)
        val fallbackArticles = fallbackResult.getOrNull() ?: emptyList()
        Log.d(TAG, "[$symbol] fallback → ${fallbackArticles.size} articole")

        return if (fallbackArticles.isNotEmpty()) {
            Result.success(fallbackArticles)
        } else {
            primaryResult.exceptionOrNull()?.let { Result.failure(it) }
                ?: Result.failure(Exception("Nu s-au gasit stiri pentru $symbol"))
        }
    }

    /** Executa un request getEverything si returneaza Result<List<Article>> */
    private suspend fun doFetch(query: String, symbol: String): Result<List<Article>> {
        return try {
            val response = api.getEverything(
                query    = query,
                sortBy   = "publishedAt",
                pageSize = 10
            )

            Log.d(TAG, "[$symbol] HTTP ${response.code()} pentru query='$query'")

            if (response.isSuccessful) {
                val articles = response.body()?.articles
                    ?.filter { article ->
                        article.title != "[Removed]"
                            && !article.title.isNullOrBlank()
                            && article.url.isNotBlank()
                    }
                    ?: emptyList()
                Result.success(articles)
            } else {
                val errorMsg = when (response.code()) {
                    401  -> "API Key invalid (401)"
                    426  -> "Plan NewsAPI insuficient (426)"
                    429  -> "Prea multe cereri (429) — incearca mai tarziu"
                    else -> "Eroare server ${response.code()}"
                }
                Log.e(TAG, "[$symbol] $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$symbol] Exceptie la fetch: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Returneaza label-ul categoriei pentru un simbol */
    fun getCategoryLabel(symbol: String): String =
        categoryLabels[symbol] ?: "TRADING"
}
