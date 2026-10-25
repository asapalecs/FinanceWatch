package com.financewatch.traderapp.ui.markets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.financewatch.traderapp.data.local.entity.MarketPreferenceEntity
import com.financewatch.traderapp.databinding.ItemMarketBinding

class MarketAdapter(
    private val onToggle: (marketId: String, isEnabled: Boolean) -> Unit
) : ListAdapter<MarketPreferenceEntity, MarketAdapter.ViewHolder>(DiffCallback()) {

    // Emoji-uri pentru fiecare piata
    private val marketEmojis = mapOf(
        "nasdaq" to "📈",
        "gold"   to "🥇",
        "forex"  to "💱",
        "crypto" to "₿",
        "general" to "📰"
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemMarketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemMarketBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: MarketPreferenceEntity) {
            b.tvEmoji.text = marketEmojis[item.marketId] ?: "📊"
            b.tvMarketName.text = item.displayName
            b.tvQuery.text = item.queryKeyword

            // Seteaza starea switch-ului fara a declansa listener-ul (evita recursie)
            b.switchEnabled.setOnCheckedChangeListener(null)
            b.switchEnabled.isChecked = item.isEnabled
            b.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(item.marketId, isChecked)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MarketPreferenceEntity>() {
        override fun areItemsTheSame(a: MarketPreferenceEntity, b: MarketPreferenceEntity) = a.marketId == b.marketId
        override fun areContentsTheSame(a: MarketPreferenceEntity, b: MarketPreferenceEntity) = a == b
    }
}
