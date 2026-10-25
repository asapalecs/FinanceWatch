package com.financewatch.traderapp.ui.bookmarks

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.financewatch.traderapp.data.local.AppDatabase
import com.financewatch.traderapp.data.local.entity.SavedNewsEntity
import com.financewatch.traderapp.data.remote.RetrofitClient
import com.financewatch.traderapp.databinding.FragmentBookmarksBinding
import com.financewatch.traderapp.repository.NewsRepository
import com.financewatch.traderapp.ui.news.NewsAdapter
import com.financewatch.traderapp.data.local.entity.NewsEntity
import com.financewatch.traderapp.viewmodel.BookmarksViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Fragment cu stirile salvate (bookmarks).
 * Suporta stergere prin swipe-to-delete cu optiunea Undo.
 */
class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookmarksViewModel by viewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = NewsRepository(db.newsDao(), db.savedNewsDao(), RetrofitClient.newsApi)
        BookmarksViewModel.Factory(repo)
    }

    private lateinit var adapter: BookmarkAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BookmarkAdapter(
            onClick = { news ->
                // Click pe card → deschide in browser
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(news.url))
                startActivity(intent)
            },
            onUnsave = { news ->
                // Tap pe iconita bookmark → sterge cu posibilitate Undo
                viewModel.deleteNews(news.id)
                Snackbar.make(binding.root, "Stire eliminata din salvate", Snackbar.LENGTH_LONG)
                    .setAction("Anuleaza") { viewModel.restoreNews(news) }
                    .show()
            }
        )

        binding.recyclerView.apply {
            this.adapter = this@BookmarksFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Swipe-to-delete
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                if (pos == RecyclerView.NO_POSITION) return  // guard crash
                val deleted = adapter.currentList[pos]
                viewModel.deleteNews(deleted.id)

                // Snackbar cu Undo functional
                Snackbar.make(binding.root, "Stire eliminata din salvate", Snackbar.LENGTH_LONG)
                    .setAction("Anuleaza") { viewModel.restoreNews(deleted) }
                    .show()
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.savedNews.collect { list ->
                    adapter.submitList(list)
                    binding.tvEmptyState.visibility =
                        if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
