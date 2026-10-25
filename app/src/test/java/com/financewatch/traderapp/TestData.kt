package com.financewatch.traderapp

import com.financewatch.traderapp.data.local.entity.NewsEntity
import com.financewatch.traderapp.data.local.entity.SavedNewsEntity
import com.financewatch.traderapp.data.local.entity.MarketPreferenceEntity
import com.financewatch.traderapp.data.remote.model.Article
import com.financewatch.traderapp.data.remote.model.NewsResponse
import com.financewatch.traderapp.data.remote.model.Source

/**
 * Factory functions pentru date de test.
 * Centralizate aici pentru a fi reutilizate in toate fisierele de test.
 */

fun createNewsEntity(
    id: String = "test-id-1",
    title: String = "Test News Title",
    description: String? = "Test description for the news article",
    url: String = "https://example.com/news/1",
    urlToImage: String? = "https://example.com/image.jpg",
    publishedAt: String = "2026-05-15T10:00:00Z",
    sourceName: String = "Test Source",
    category: String = "nasdaq",
    cachedAt: Long = System.currentTimeMillis()
) = NewsEntity(
    id = id,
    title = title,
    description = description,
    url = url,
    urlToImage = urlToImage,
    publishedAt = publishedAt,
    sourceName = sourceName,
    category = category,
    cachedAt = cachedAt
)

fun createSavedNewsEntity(
    id: String = "saved-id-1",
    title: String = "Saved News Title",
    category: String = "nasdaq"
) = SavedNewsEntity(
    id = id,
    title = title,
    description = "Saved description",
    url = "https://example.com/saved/1",
    urlToImage = null,
    publishedAt = "2026-05-15T10:00:00Z",
    sourceName = "Test Source",
    category = category,
    savedAt = System.currentTimeMillis()
)

fun createMarketPreference(
    marketId: String = "nasdaq",
    displayName: String = "Nasdaq / S&P500",
    isEnabled: Boolean = true,
    queryKeyword: String = "Nasdaq OR S&P500"
) = MarketPreferenceEntity(marketId, displayName, isEnabled, queryKeyword)

fun createArticle(
    title: String = "Article Title",
    url: String = "https://example.com/article",
    sourceName: String = "Reuters"
) = Article(
    source = Source(id = null, name = sourceName),
    author = "Test Author",
    title = title,
    description = "Article description",
    url = url,
    urlToImage = null,
    publishedAt = "2026-05-15T10:00:00Z",
    content = "Full article content..."
)

fun createNewsResponse(articles: List<Article> = listOf(createArticle())) = NewsResponse(
    status = "ok",
    totalResults = articles.size,
    articles = articles
)
