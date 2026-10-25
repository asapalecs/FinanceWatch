package com.financewatch.traderapp.data.local.dao

import androidx.room.*
import com.financewatch.traderapp.data.local.entity.SavedNewsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO pentru stirile salvate de utilizator (bookmarks).
 * Stirile salvate nu expira si raman disponibile offline.
 */
@Dao
interface SavedNewsDao {

    /**
     * Returneaza toate stirile salvate, ordonate de la cele mai recent salvate.
     */
    @Query("SELECT * FROM saved_news ORDER BY saved_at DESC")
    fun getAllSaved(): Flow<List<SavedNewsEntity>>

    /**
     * Salveaza o stire. Daca exista deja (acelasi ID), o ignora (IGNORE).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun save(news: SavedNewsEntity)

    /**
     * Sterge o stire salvata dupa ID.
     */
    @Query("DELETE FROM saved_news WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Verifica daca o stire este salvata. Emite true/false reactiv.
     * Folosit in NewsDetailFragment pentru starea butonului bookmark.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM saved_news WHERE id = :id)")
    fun isSaved(id: String): Flow<Boolean>

    /**
     * Sterge toate stirile salvate (optiune de reset din Settings).
     */
    @Query("DELETE FROM saved_news")
    suspend fun deleteAll()

    /**
     * Returneaza numarul de stiri salvate.
     */
    @Query("SELECT COUNT(*) FROM saved_news")
    fun getSavedCount(): Flow<Int>
}
