package com.financewatch.traderapp.data.remote

import com.financewatch.traderapp.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton care configureaza si expune instanta Retrofit.
 * OkHttp este configurat cu:
 *  - Timeout 30 secunde pentru conectare si citire
 *  - Logging interceptor (activ pentru debugging)
 *  - API Key interceptor (adauga header-ul X-Api-Key la toate request-urile)
 *
 * Cheia API este citita din BuildConfig (setata in local.properties ca NEWS_API_KEY=xxx)
 */
object RetrofitClient {

    private const val BASE_URL = "https://newsapi.org/v2/"

    // Interceptor pentru logarea request-urilor HTTP (vizibil in Logcat)
    // Activ pe BODY level pentru debugging complet
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Interceptor pentru injectarea API key-ului din BuildConfig
    private val apiKeyInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .header("X-Api-Key", BuildConfig.NEWS_API_KEY)
            .build()
        chain.proceed(newRequest)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(apiKeyInterceptor)
        .build()

    // Instanta Retrofit creata lazy (prima data cand e accesata)
    val newsApi: NewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }
}
