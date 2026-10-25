package com.financewatch.traderapp.ui.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.financewatch.traderapp.R
import com.financewatch.traderapp.data.local.AppDatabase
import com.financewatch.traderapp.data.local.entity.NewsEntity
import com.financewatch.traderapp.data.remote.RetrofitClient
import com.financewatch.traderapp.databinding.FragmentNewsDetailBinding
import com.financewatch.traderapp.repository.NewsRepository
import com.financewatch.traderapp.viewmodel.NewsViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Ecranul de detaliu al unei stiri.
 * FAB bookmark salveaza/elimina stirea din Room + Firestore cu feedback vizual.
 */
class NewsDetailFragment : Fragment() {

    private var _binding: FragmentNewsDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsViewModel by viewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = NewsRepository(db.newsDao(), db.savedNewsDao(), RetrofitClient.newsApi)
        NewsViewModel.Factory(repo)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentNewsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val newsId      = arguments?.getString("newsId") ?: ""
        val title       = arguments?.getString("title") ?: ""
        val url         = arguments?.getString("url") ?: ""
        val imageUrl    = arguments?.getString("imageUrl") ?: ""
        val sourceName  = arguments?.getString("sourceName") ?: ""
        val publishedAt = arguments?.getString("publishedAt") ?: ""
        val description = arguments?.getString("description") ?: ""
        val category    = arguments?.getString("category") ?: "general"

        // Populeaza UI
        binding.tvTitle.text = title
        binding.tvSource.text = "$sourceName · $publishedAt"
        binding.tvDescription.text = description.ifEmpty { "Apasa 'Citeste articolul' pentru continut complet." }

        if (imageUrl.isNotEmpty()) {
            Glide.with(this).load(imageUrl)
                .placeholder(R.drawable.ic_news_placeholder)
                .into(binding.ivImage)
        } else {
            binding.ivImage.setImageResource(R.drawable.ic_news_placeholder)
        }

        // Buton articol complet
        binding.btnReadFull.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // Construieste o NewsEntity temporara pentru operatiile de save
        val newsEntity = NewsEntity(
            id          = newsId,
            title       = title,
            description = description,
            url         = url,
            urlToImage  = imageUrl,
            publishedAt = publishedAt,
            sourceName  = sourceName,
            category    = category,
            cachedAt    = System.currentTimeMillis()
        )

        // Observa starea saved → actualizeaza iconita FAB
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.savedNewsIds.collect { savedIds ->
                    val isSaved = newsId in savedIds
                    binding.fabBookmark.setImageResource(
                        if (isSaved) R.drawable.ic_bookmark_filled
                        else         R.drawable.ic_bookmark_border
                    )
                }
            }
        }

        // FAB toggle save/unsave
        binding.fabBookmark.setOnClickListener {
            val wasSaved = newsId in viewModel.savedNewsIds.value
            viewModel.toggleSave(newsEntity)
            val msg = if (wasSaved) "Eliminat din salvate" else "Salvat cu succes!"
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
