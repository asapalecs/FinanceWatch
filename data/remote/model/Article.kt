package com.financewatch.traderapp.data.remote.model

import com.financewatch.traderapp.data.local.entity.NewsEntity

// ---------------------------------------------------------------------------
// Modele de date primite de la NewsAPI (stratul remote)
// Folosite de Retrofit pentru deserializare prin Gson
// ---------------------------------------------------------------------------

/**
 * Sursa articolului (publicatia sau platforma care a publicat stirea).
 * @param id Identificatorul unic al sursei (poate fi null pentru surse necunoscute)
 * @param name Numele sursei (ex: "Bloomberg", "Reuters")
 */
data class Source(
    val id: String?,
    val name: String
)

/**
 * Reprezinta un articol de stiri primit de la NewsAPI.
 * Campurile nullable reflecta comportamentul real al API-ului —
 * unele articole nu au toate campurile completate.
 *
 * @param source Sursa articolului
 * @param author Autorul (poate fi null)
 * @param title Titlul articolului
 * @param description Descriere scurta (poate fi null)
 * @param url URL catre articolul original
 * @param urlToImage URL catre imaginea asociata (poate fi null)
 * @param publishedAt Data publicarii in format ISO 8601
 * @param content Continut partial al articolului (poate fi null, trunchiat de API la 200 chars)
 */
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

// ---------------------------------------------------------------------------
// Extension function: conversie Article -> NewsEntity (entitate Room)
// Separare clara intre stratul remote si stratul local
// ---------------------------------------------------------------------------

/**
 * Transforma un [Article] din raspunsul API intr-un [NewsEntity] pentru stocarea locala.
 *
 * @param category Categoria stirii (ex: "nasdaq", "gold", "crypto")
 * @return [NewsEntity] gata de inserare in baza de date locala Room
 */
fun Article.toNewsEntity(category: String): NewsEntity {
    return NewsEntity(
        // ID unic generat din URL pentru a evita duplicate
        id = url.hashCode().toString(),
        title = title,
        description = description ?: "",
        url = url,
        urlToImage = urlToImage,
        publishedAt = publishedAt,
        sourceName = source.name,
        author = author,
        content = content,
        category = category,
        // Timestamp local al momentului cand a fost salvat in cache
        cachedAt = System.currentTimeMillis()
    )
}
