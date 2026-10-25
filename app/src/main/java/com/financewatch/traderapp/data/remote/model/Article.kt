package com.financewatch.traderapp.data.remote.model

import com.financewatch.traderapp.data.local.entity.NewsEntity
import java.util.UUID

data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<Article>
)

data class Article(
    val source: Source,
    val author: String?,
    val title: String,
    val description: String?,
    val url: String,
    val urlToImage: String?,
    val publishedAt: String,
    val content: String?
)

data class Source(
    val id: String?,
    val name: String
)

/**
 * Functie de extensie: transforma un Article (DTO din API) intr-un NewsEntity (Room).
 * ID-ul e generat din URL (hash) pentru a evita duplicate.
 */
fun Article.toNewsEntity(category: String): NewsEntity {
    return NewsEntity(
        id = url.hashCode().toString(),
        title = title,
        description = description,
        url = url,
        urlToImage = urlToImage,
        publishedAt = publishedAt,
        sourceName = source.name,
        category = category,
        cachedAt = System.currentTimeMillis()
    )
}
