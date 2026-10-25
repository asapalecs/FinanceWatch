package com.financewatch.traderapp.ui.news

import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.financewatch.traderapp.R
import com.financewatch.traderapp.data.local.entity.NewsEntity
import com.financewatch.traderapp.databinding.ItemNewsCardBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Adapter pentru RecyclerView cu stiri.
 * Suporta:
 *  - click pe card → detaliu
 *  - click pe butonul bookmark → toggle save/unsave cu feedback vizual
 *  - starea vizuala (bookmark plin/gol) determinata de savedIds
 */
class NewsAdapter(
    private val onClick: (NewsEntity) -> Unit,
    private val onSaveClick: (NewsEntity) -> Unit = {}
) : ListAdapter<NewsEntity, NewsAdapter.NewsViewHolder>(DiffCallback()) {

    /** Set-ul de ID-uri salvate — actualizat din Fragment cand StateFlow emite */
    var savedIds: Set<String> = emptySet()
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SAVE_STATE)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position), getItem(position).id in savedIds)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.contains(PAYLOAD_SAVE_STATE)) {
            // Actualizeaza doar iconita bookmark, fara a rebind tot card-ul
            holder.updateSaveIcon(getItem(position).id in savedIds)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class NewsViewHolder(private val binding: ItemNewsCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(news: NewsEntity, isSaved: Boolean) {
            binding.tvTitle.text = news.title
            binding.tvSource.text = "${news.sourceName} · ${formatDate(news.publishedAt)}"
            binding.tvCategory.text = news.category.uppercase()

            // Culoare chip categorie
            val chipColor = when (news.category) {
                "nasdaq" -> R.color.nasdaq
                "gold"   -> R.color.gold
                "forex"  -> R.color.forex
                "crypto" -> R.color.crypto
                else     -> R.color.general
            }
            binding.tvCategory.setBackgroundColor(ContextCompat.getColor(binding.root.context, chipColor))

            // Imagine
            Glide.with(binding.ivImage)
                .load(news.urlToImage)
                .placeholder(R.drawable.ic_news_placeholder)
                .error(R.drawable.ic_news_placeholder)
                .centerCrop()
                .into(binding.ivImage)

            // Starea initiala a bookmark-ului
            updateSaveIcon(isSaved)

            // Click pe card → detaliu
            binding.root.setOnClickListener { onClick(news) }

            // Click pe bookmark → toggle
            binding.btnSave.setOnClickListener { onSaveClick(news) }
        }

        /** Actualizeaza doar iconita — apelat si via payload pentru eficienta */
        fun updateSaveIcon(isSaved: Boolean) {
            binding.btnSave.setImageResource(
                if (isSaved) R.drawable.ic_bookmark_filled
                else         R.drawable.ic_bookmark_border
            )
        }

        private fun formatDate(dateString: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(dateString) ?: return dateString
                val diffHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - date.time)
                when {
                    diffHours < 1  -> "acum"
                    diffHours < 24 -> "acum ${diffHours}h"
                    diffHours < 48 -> "ieri"
                    else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) { dateString }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NewsEntity>() {
        override fun areItemsTheSame(old: NewsEntity, new: NewsEntity) = old.id == new.id
        override fun areContentsTheSame(old: NewsEntity, new: NewsEntity) = old == new
    }

    companion object {
        private const val PAYLOAD_SAVE_STATE = "SAVE_STATE"
    }
}
