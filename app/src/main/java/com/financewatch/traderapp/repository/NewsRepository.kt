package com.financewatch.traderapp.repository

import com.financewatch.traderapp.data.local.dao.NewsDao
import com.financewatch.traderapp.data.local.dao.SavedNewsDao
import com.financewatch.traderapp.data.local.entity.NewsEntity
import com.financewatch.traderapp.data.local.entity.SavedNewsEntity
import com.financewatch.traderapp.data.remote.NewsApiService
import com.financewatch.traderapp.data.remote.model.NewsResponse
import com.financewatch.traderapp.data.remote.model.toNewsEntity
import retrofit2.Response
import kotlinx.coroutines.flow.Flow

/**
 * Repository-ul central al aplicatiei.
 *
 * Implementeaza pattern-ul "offline-first":
 *  1. Emite imediat datele din cache (Room) — UI se actualizeaza instant
 *  2. Fetch din API in fundal
 *  3. Salveaza in Room — UI se actualizeaza automat prin Flow
 *  4. Daca API esueaza (offline) → continua cu datele din cache
 *
 * Aceasta arhitectura garanteaza ca aplicatia functioneaza si fara internet.
 */
class NewsRepository(
    private val newsDao: NewsDao,
    private val savedNewsDao: SavedNewsDao,
    private val apiService: NewsApiService,
    private val firestoreRepo: FirestoreRepository = FirestoreRepository()
) {

    /**
     * Mapeaza categoria interna a aplicatiei la parametrii corecti ai NewsAPI.
     *
     * NewsAPI top-headlines accepta doar categoriile oficiale:
     * business, entertainment, general, health, science, sports, technology
     *
     * Pentru categoriile financiare specifice (nasdaq, gold, forex, crypto)
     * folosim endpoint-ul /everything cu query-uri de cuvinte cheie.
     */
    private fun fetchNewsForCategory(category: String): suspend () -> Response<NewsResponse> {
        return when (category.lowercase()) {
            "nasdaq"  -> { { apiService.getEverything(query = "nasdaq OR \"S&P 500\" OR \"stock market\" OR \"wall street\"") } }
            "gold"    -> { { apiService.getEverything(query = "\"gold price\" OR \"spot gold\" OR \"gold futures\" OR XAUUSD OR \"XAU/USD\" OR bullion") } }
            "forex"   -> { { apiService.getEverything(query = "forex OR \"currency exchange\" OR EUR/USD OR USD/JPY OR \"foreign exchange\"") } }
            "crypto"  -> { { apiService.getEverything(query = "cryptocurrency OR bitcoin OR ethereum OR \"crypto market\"") } }
            else      -> { { apiService.getTopHeadlines(category = "business") } }
        }
    }

    /**
     * Returneaza stiri din cache pentru o categorie.
     */
    fun getCachedNews(category: String): Flow<List<NewsEntity>> {
        return newsDao.getNewsByCategory(category)
    }

    /**
     * Returneaza toate stirile din cache.
     */
    fun getAllCachedNews(): Flow<List<NewsEntity>> {
        return newsDao.getAllNews()
    }

    /**
     * Salveaza o stire in Room si o sincronizeaza cu Firestore.
     */
    suspend fun saveNews(newsEntity: NewsEntity) {
        val savedNews = SavedNewsEntity(
            id          = newsEntity.id,
            title       = newsEntity.title,
            description = newsEntity.description,
            url         = newsEntity.url,
            urlToImage  = newsEntity.urlToImage,
            publishedAt = newsEntity.publishedAt,
            sourceName  = newsEntity.sourceName,
            category    = newsEntity.category,
            savedAt     = System.currentTimeMillis()
        )
        savedNewsDao.save(savedNews)
        firestoreRepo.syncSave(savedNews)  // sync cloud in background
    }

    /**
     * Salveaza din SavedNewsEntity direct (folosit la sync Firestore → Room).
     */
    suspend fun saveNewsEntity(entity: SavedNewsEntity) {
        savedNewsDao.save(entity)
    }

    /**
     * Sterge o stire din Room si din Firestore.
     */
    suspend fun unsaveNews(newsId: String) {
        savedNewsDao.delete(newsId)
        firestoreRepo.syncDelete(newsId)   // sync cloud in background
    }

    /**
     * La login: aduce stirile din Firestore si le insereaza in Room.
     */
    suspend fun syncFromFirestore() {
        val cloudItems = firestoreRepo.fetchSavedFromCloud()
        cloudItems.forEach { savedNewsDao.save(it) }
    }

    /**
     * Returneaza toate stirile salvate.
     */
    fun getSavedNews(): Flow<List<SavedNewsEntity>> {
        return savedNewsDao.getAllSaved()
    }

    /**
     * Verifica daca o stire este salvata.
     */
    fun isSaved(newsId: String): Flow<Boolean> {
        return savedNewsDao.isSaved(newsId)
    }

    /**
     * Reincarca stirile pentru o categorie si returneaza result-ul (pentru NewsSyncService).
     */
    suspend fun refreshNews(category: String): Result<List<NewsEntity>> {
        return try {
            val response = fetchNewsForCategory(category).invoke()
            if (response.isSuccessful && response.body() != null) {
                val newsResponse = response.body()!!
                val entities = newsResponse.articles.map { it.toNewsEntity(category) }
                newsDao.replaceCategory(category, entities)
                Result.success(entities)
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "API Key invalid sau expirat (401)"
                    429 -> "Prea multe cereri catre API (429) - incearca mai tarziu"
                    else -> "Eroare API: ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Curata stirile mai vechi de 24 ore din cache (pentru NewsSyncService).
     */
    suspend fun clearOldCache() {
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        newsDao.deleteOlderThan(oneDayAgo)
    }
}

