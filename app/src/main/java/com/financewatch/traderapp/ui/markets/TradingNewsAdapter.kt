package com.financewatch.traderapp.ui.markets

import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.financewatch.traderapp.R
import com.financewatch.traderapp.data.remote.model.Article
import com.financewatch.traderapp.databinding.ItemTradingNewsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Adapter pentru stiri de trading din tabul Piete.
 *
 * Fiecare articol afiseaza:
 *  - Thumbnail (Glide)
 *  - Chip categorie colorat (CRYPTO/FOREX/GOLD/NASDAQ)
 *  - Titlu (max 2 linii)
 *  - Sursa + timestamp relativ (acum X ore/zile)
 */
class TradingNewsAdapter(
    private val categoryLabel: String,
    private val onArticleClick: (Article) -> Unit
) : ListAdapter<Article, TradingNewsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTradingNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemTradingNewsBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(article: Article) {
            b.tvTitle.text  = article.title
            b.tvSource.text = article.source.name
            b.tvCategory.text = categoryLabel
            b.tvTime.text = formatRelativeTime(article.publishedAt)

            // Culoare chip per categorie
            val chipColor = when (categoryLabel) {
                "CRYPTO" -> R.color.crypto
                "FOREX"  -> R.color.forex
                "GOLD"   -> R.color.gold
                "NASDAQ" -> R.color.nasdaq
                else     -> R.color.general
            }
            b.tvCategory.setBackgroundColor(ContextCompat.getColor(b.root.context, chipColor))

            // Thumbnail
            Glide.with(b.ivThumbnail)
                .load(article.urlToImage)
                .placeholder(R.drawable.ic_news_placeholder)
                .centerCrop()
                .into(b.ivThumbnail)

            b.root.setOnClickListener { onArticleClick(article) }
        }
    }

    // ── Timestamp relativ ──────────────────────────────────────────────────────

    /**
     * Converteste ISO 8601 → "acum 2h", "acum 1zi", etc.
     */
    private fun formatRelativeTime(publishedAt: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date: Date = sdf.parse(publishedAt) ?: return publishedAt

            val diffMs   = System.currentTimeMillis() - date.time
            val diffMins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
            val diffHrs  = TimeUnit.MILLISECONDS.toHours(diffMs)
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)

            when {
                diffMins < 60  -> "${diffMins}m"
                diffHrs  < 24  -> "${diffHrs}h"
                diffDays < 7   -> "${diffDays}z"
                else           -> SimpleDateFormat("dd MMM", Locale.US).format(date)
            }
        } catch (_: Exception) {
            ""
        }
    }

    // ── DiffCallback ───────────────────────────────────────────────────────────

    class DiffCallback : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(a: Article, b: Article)    = a.url == b.url
        override fun areContentsTheSame(a: Article, b: Article) = a == b
    }
}
