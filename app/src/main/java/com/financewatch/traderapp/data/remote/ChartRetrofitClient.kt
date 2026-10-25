package com.financewatch.traderapp.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit pentru Yahoo Finance v8 API.
 * Base URL diferit fata de NewsAPI — instanta separata.
 * Nu are nevoie de API key interceptor, dar are nevoie de User-Agent
 * pentru a evita blocarea de catre Yahoo Finance.
 */
object ChartRetrofitClient {

    private const val BASE_URL = "https://query1.finance.yahoo.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    // Yahoo Finance blocheaza request-urile fara User-Agent de browser valid
    private val userAgentInterceptor = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile) AppleWebKit/537.36 Chrome/120.0 Safari/537.36")
                .header("Accept", "application/json")
                .build()
        )
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(userAgentInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val chartApi: ChartApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChartApiService::class.java)
    }
}
