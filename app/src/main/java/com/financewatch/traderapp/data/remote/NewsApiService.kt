package com.financewatch.traderapp.data.remote

import com.financewatch.traderapp.data.remote.model.NewsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interfata Retrofit pentru NewsAPI.org.
 * Base URL: https://newsapi.org/v2/
 *
 * API Key-ul este injectat automat de un OkHttp interceptor.
 * Documentatie API: https://newsapi.org/docs/endpoints
 */
interface NewsApiService {

    /**
     * Cauta articole dupa cuvinte cheie.
     * Folosit pentru pietele specifice: Nasdaq, XAUUSD, EURUSD, Crypto.
     */
    @GET("everything")
    suspend fun getEverything(
        @Query("q") query: String,
        @Query("searchIn") searchIn: String = "title,description",
        @Query("language") language: String = "en",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 20
    ): Response<NewsResponse>

    /**
     * Returneaza top headline-uri pentru o categorie.
     * Folosit pentru stiri generale financiare/business.
     */
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("category") category: String = "business",
        @Query("language") language: String = "en",
        @Query("pageSize") pageSize: Int = 20
    ): Response<NewsResponse>
}
