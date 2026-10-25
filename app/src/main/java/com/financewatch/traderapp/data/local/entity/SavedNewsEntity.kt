package com.financewatch.traderapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitate Room pentru stirile salvate de utilizator (bookmarks).
 * Persista indiferent de expirarea cache-ului normal.
 */
@Entity(tableName = "saved_news")
data class SavedNewsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "url_to_image")
    val urlToImage: String?,

    @ColumnInfo(name = "published_at")
    val publishedAt: String,

    @ColumnInfo(name = "source_name")
    val sourceName: String,

    @ColumnInfo(name = "category")
    val category: String,

    // Timestamp cand utilizatorul a salvat stirea
    @ColumnInfo(name = "saved_at")
    val savedAt: Long = System.currentTimeMillis()
)
