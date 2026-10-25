package com.financewatch.traderapp.ui.bookmarks

import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.financewatch.traderapp.R
import com.financewatch.traderapp.data.local.entity.SavedNewsEntity
import com.financewatch.traderapp.databinding.ItemNewsCardBinding

class BookmarkAdapter(
    private val onClick: (SavedNewsEntity) -> Unit,
    private val onUnsave: (SavedNewsEntity) -> Unit
) : ListAdapter<SavedNewsEntity, BookmarkAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemNewsCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemNewsCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SavedNewsEntity) {
            binding.tvTitle.text = item.title
            binding.tvSource.text = item.sourceName
            binding.tvCategory.text = item.category.uppercase()

            // Culoare chip categorie
            val chipColor = when (item.category) {
                "nasdaq" -> R.color.nasdaq
                "gold"   -> R.color.gold
                "forex"  -> R.color.forex
                "crypto" -> R.color.crypto
                else     -> R.color.general
            }
            binding.tvCategory.setBackgroundColor(ContextCompat.getColor(binding.root.context, chipColor))

            Glide.with(binding.ivImage).load(item.urlToImage)
                .placeholder(R.drawable.ic_news_placeholder).centerCrop().into(binding.ivImage)

            // In tabul Salvate toate stirile sunt salvate → icon plin
            binding.btnSave.setImageResource(R.drawable.ic_bookmark_filled)

            // Tap pe iconita bookmark → sterge din salvate
            binding.btnSave.setOnClickListener { onUnsave(item) }

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SavedNewsEntity>() {
        override fun areItemsTheSame(a: SavedNewsEntity, b: SavedNewsEntity) = a.id == b.id
        override fun areContentsTheSame(a: SavedNewsEntity, b: SavedNewsEntity) = a == b
    }
}
