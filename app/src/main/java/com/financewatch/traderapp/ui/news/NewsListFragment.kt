package com.financewatch.traderapp.ui.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.financewatch.traderapp.R
import com.financewatch.traderapp.data.local.AppDatabase
import com.financewatch.traderapp.data.remote.RetrofitClient
import com.financewatch.traderapp.databinding.FragmentNewsListBinding
import com.financewatch.traderapp.repository.NewsRepository
import com.financewatch.traderapp.viewmodel.NewsViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Fragment-ul principal cu lista de stiri.
 * Afiseaza stiri filtrate pe categorie intr-un RecyclerView.
 * Suporta: filtrare prin Chips, pull-to-refresh, empty state, error state.
 */
class NewsListFragment : Fragment() {

    private var _binding: FragmentNewsListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsViewModel by viewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = NewsRepository(db.newsDao(), db.savedNewsDao(), RetrofitClient.newsApi)
        NewsViewModel.Factory(repo)
    }

    private lateinit var newsAdapter: NewsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNewsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCategoryChips()
        setupSwipeRefresh()
        observeViewModel()

        // Fetch initial
        viewModel.refresh()
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(
            onClick = { news ->
                val bundle = bundleOf(
                    "newsId"      to news.id,
                    "title"       to news.title,
                    "url"         to news.url,
                    "imageUrl"    to (news.urlToImage ?: ""),
                    "sourceName"  to news.sourceName,
                    "publishedAt" to news.publishedAt,
                    "description" to (news.description ?: ""),
                    "category"    to news.category
                )
                findNavController().navigate(R.id.action_newsList_to_newsDetail, bundle)
            },
            onSaveClick = { news ->
                val wasSaved = news.id in viewModel.savedNewsIds.value
                viewModel.toggleSave(news)
                val msg = if (wasSaved) "Stire eliminata din salvate" else "Stire salvata!"
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
            }
        )

        binding.recyclerView.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupCategoryChips() {
        val categories = mapOf(
            "nasdaq" to "Nasdaq",
            "gold"   to "Aur",
            "forex"  to "Forex",
            "crypto" to "Crypto",
            "general" to "General"
        )

        categories.forEach { (id, label) ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = id == "nasdaq"
                setOnClickListener { viewModel.selectCategory(id) }
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observa lista de stiri
                launch {
                    viewModel.news.collect { newsList ->
                        newsAdapter.submitList(newsList)
                        binding.tvEmptyState.visibility =
                            if (newsList.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                // Observa stirile salvate → actualizeaza icoanele din adapter
                launch {
                    viewModel.savedNewsIds.collect { ids ->
                        newsAdapter.savedIds = ids
                    }
                }

                // Observa starea de incarcare
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.swipeRefreshLayout.isRefreshing = loading
                    }
                }

                // Observa erorile (one-shot events)
                launch {
                    viewModel.errorEvent.collect { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evita memory leak
    }
}
