package com.financewatch.traderapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitate Room pentru cache-ul stirilor descarcate din API.
 * Stirile sunt stocate local pentru acces offline.
 */
@Entity(tableName = "news_cache")
data class NewsEntity(
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

    // Categoria stirii: "nasdaq", "gold", "forex", "crypto", "general"
    @ColumnInfo(name = "category")
    val category: String,

    // Timestamp cand a fost salvata in cache (pentru expirare dupa 24h)
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
