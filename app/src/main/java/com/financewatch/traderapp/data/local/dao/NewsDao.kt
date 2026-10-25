package com.financewatch.traderapp.data.local.dao

import androidx.room.*
import com.financewatch.traderapp.data.local.entity.NewsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) pentru stirile din cache.
 * Toate operatiile de citire returneaza Flow pentru a reactiona
 * automat la schimbarile din baza de date (arhitectura reactiva).
 */
@Dao
interface NewsDao {

    /**
     * Returneaza stirile filtrate pe categorie, ordonate de la cele mai noi.
     * Flow emite automat o noua lista cand datele se schimba.
     */
    @Query("SELECT * FROM news_cache WHERE category = :category ORDER BY published_at DESC LIMIT 100")
    fun getNewsByCategory(category: String): Flow<List<NewsEntity>>

    /**
     * Returneaza toate stirile din cache, indiferent de categorie.
     */
    @Query("SELECT * FROM news_cache ORDER BY published_at DESC LIMIT 100")
    fun getAllNews(): Flow<List<NewsEntity>>

    /**
     * Insereaza o lista de stiri. Daca o stire cu acelasi ID exista deja,
     * o inlocuieste (REPLACE) — util pentru actualizare date.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(news: List<NewsEntity>)

    /**
     * Sterge stirile mai vechi de un anumit timestamp.
     * Folosit pentru curatarea cache-ului dupa 24 de ore.
     */
    @Query("DELETE FROM news_cache WHERE cached_at < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    /**
     * Returneaza numarul total de stiri din cache.
     */
    @Query("SELECT COUNT(*) FROM news_cache")
    fun getNewsCount(): Flow<Int>

    /**
     * Sterge toate stirile dintr-o categorie (util la refresh fortat).
     */
    @Query("DELETE FROM news_cache WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Transaction
    suspend fun replaceCategory(category: String, news: List<NewsEntity>) {
        deleteByCategory(category)
        insertAll(news)
    }

    /**
     * Returneaza o singura stire dupa ID.
     */
    @Query("SELECT * FROM news_cache WHERE id = :id LIMIT 1")
    suspend fun getNewsById(id: String): NewsEntity?
}
