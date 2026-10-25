package com.financewatch.traderapp.repository

import com.financewatch.traderapp.data.local.entity.SavedNewsEntity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Repository pentru sincronizarea stirilor salvate cu Firebase Firestore.
 *
 * Structura Firestore:
 *   users/{uid}/saved_news/{newsId}  →  campurile SavedNewsEntity
 *
 * Strategia este "local-first":
 *  - Salveaza/sterge imediat in Room (offline-first)
 *  - Incearca sync cu Firestore in background
 *  - Daca Firebase nu e disponibil, operatia reuseste oricum local
 */
class FirestoreRepository {

    private val db   = Firebase.firestore
    private val auth = Firebase.auth

    /** Returneaza referinta la colectia saved_news a utilizatorului curent, sau null daca nu e logat. */
    private fun savedRef() = auth.currentUser?.uid?.let { uid ->
        db.collection("users").document(uid).collection("saved_news")
    }

    /**
     * Sincronizeaza o stire salvata catre Firestore.
     * Apelat dupa ce s-a salvat cu succes in Room.
     * Esecurile sunt inghitite silentios (offline-first).
     */
    suspend fun syncSave(news: SavedNewsEntity) {
        try {
            savedRef()?.document(news.id)?.set(news.toMap())?.await()
        } catch (_: Exception) {
            // Offline sau utilizator delogat — Room e sursa de adevar
        }
    }

    /**
     * Sterge o stire salvata din Firestore.
     * Apelat dupa ce s-a sters cu succes din Room.
     */
    suspend fun syncDelete(newsId: String) {
        try {
            savedRef()?.document(newsId)?.delete()?.await()
        } catch (_: Exception) {}
    }

    /**
     * La login sau la pornirea aplicatiei, aduce stirile salvate din Firestore
     * si le insereaza in Room (merge — nu sterge cele locale).
     * Returneaza lista de entitati sincronizate.
     */
    suspend fun fetchSavedFromCloud(): List<SavedNewsEntity> {
        return try {
            val snapshot = savedRef()?.get()?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { doc ->
                try {
                    SavedNewsEntity(
                        id          = doc.id,
                        title       = doc.getString("title") ?: return@mapNotNull null,
                        description = doc.getString("description"),
                        url         = doc.getString("url") ?: return@mapNotNull null,
                        urlToImage  = doc.getString("urlToImage"),
                        publishedAt = doc.getString("publishedAt") ?: "",
                        sourceName  = doc.getString("sourceName") ?: "",
                        category    = doc.getString("category") ?: "",
                        savedAt     = doc.getLong("savedAt") ?: System.currentTimeMillis()
                    )
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ─── Extension ────────────────────────────────────────────────────────────

    private fun SavedNewsEntity.toMap(): Map<String, Any?> = mapOf(
        "title"       to title,
        "description" to description,
        "url"         to url,
        "urlToImage"  to urlToImage,
        "publishedAt" to publishedAt,
        "sourceName"  to sourceName,
        "category"    to category,
        "savedAt"     to savedAt
    )
}
