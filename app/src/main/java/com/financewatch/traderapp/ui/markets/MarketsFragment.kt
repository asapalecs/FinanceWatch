package com.financewatch.traderapp.ui.markets

import androidx.core.content.ContextCompat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.financewatch.traderapp.data.local.AppDatabase
import com.financewatch.traderapp.data.remote.ChartRetrofitClient
import com.financewatch.traderapp.data.remote.RetrofitClient
import com.financewatch.traderapp.databinding.FragmentMarketsBinding
import com.financewatch.traderapp.repository.ChartRepository
import com.financewatch.traderapp.repository.TradingNewsRepository
import com.financewatch.traderapp.viewmodel.ChartUiState
import com.financewatch.traderapp.viewmodel.ChartViewModel
import com.financewatch.traderapp.viewmodel.TradingNewsUiState
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment cu:
 *  1. Card chart live (Yahoo Finance, fara API key) cu chip selector simbol
 *  2. RecyclerView cu pietele urmarite (toggle switch → Room)
 *  3. RecyclerView cu stiri de trading specifice perechii selectate (NewsAPI)
 */
class MarketsFragment : Fragment() {

    private var _binding: FragmentMarketsBinding? = null
    private val binding get() = _binding!!

    private val chartViewModel: ChartViewModel by viewModels {
        ChartViewModel.Factory(
            ChartRepository(ChartRetrofitClient.chartApi),
            TradingNewsRepository(RetrofitClient.newsApi)
        )
    }

    private lateinit var tradingNewsAdapter: TradingNewsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentMarketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMarketsRecyclerView()
        setupTradingNewsRecyclerView()
        setupChipSelector()
        observeChart()
        observeTradingNews()
    }

    // ─── Lista piete (toggle switch) ──────────────────────────────────────────

    private fun setupMarketsRecyclerView() {
        val db = AppDatabase.getDatabase(requireContext())
        val dao = db.marketPreferenceDao()

        val adapter = MarketAdapter { marketId, isEnabled ->
            lifecycleScope.launch { dao.updateEnabled(marketId, isEnabled) }
        }

        binding.recyclerView.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.getAllMarkets().collect { markets -> adapter.submitList(markets) }
            }
        }
    }

    // ─── Trading News RecyclerView ────────────────────────────────────────────

    private fun setupTradingNewsRecyclerView() {
        tradingNewsAdapter = TradingNewsAdapter(
            categoryLabel = "CRYPTO",
            onArticleClick = { article ->
                // Deschide articolul in browser
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(article.url)
                )
                startActivity(intent)
            }
        )

        binding.recyclerTradingNews.apply {
            adapter = tradingNewsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
    }

    // ─── Chip selector simbol ─────────────────────────────────────────────────

    private fun setupChipSelector() {
        binding.chipBtc.setOnClickListener {
            binding.tvNewsSymbolBadge.text = "BTC"
            chartViewModel.loadChart("BTC-USD")
        }
        binding.chipEth.setOnClickListener {
            binding.tvNewsSymbolBadge.text = "ETH"
            chartViewModel.loadChart("ETH-USD")
        }
        binding.chipEurUsd.setOnClickListener {
            binding.tvNewsSymbolBadge.text = "EUR/USD"
            chartViewModel.loadChart("EURUSD=X")
        }
        binding.chipGold.setOnClickListener {
            binding.tvNewsSymbolBadge.text = "GOLD"
            chartViewModel.loadChart("GC=F")
        }
        binding.chipNdx.setOnClickListener {
            binding.tvNewsSymbolBadge.text = "NDX"
            chartViewModel.loadChart("^NDX")
        }
    }

    // ─── Observa starea chart ─────────────────────────────────────────────────

    private fun observeChart() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chartViewModel.chartState.collect { state ->
                    when (state) {
                        is ChartUiState.Loading -> {
                            binding.chartProgress.visibility = View.VISIBLE
                            binding.tvChartError.visibility  = View.GONE
                            binding.lineChartView.visibility = View.INVISIBLE
                        }
                        is ChartUiState.Success -> {
                            binding.chartProgress.visibility = View.GONE
                            binding.tvChartError.visibility  = View.GONE
                            binding.lineChartView.visibility = View.VISIBLE

                            val data = state.data
                            binding.tvChartEmoji.text      = data.emoji
                            binding.tvChartSymbolName.text = data.displayName
                            binding.tvChartSymbol.text     = data.symbol

                            // Formateaza pretul cu separatoare de mii
                            val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
                                minimumFractionDigits = 2
                                maximumFractionDigits = 2
                            }
                            binding.tvChartPrice.text = "${data.currency} ${fmt.format(data.currentPrice)}"

                            val changeStr = "${data.changeSign}${"%.2f".format(data.changePercent)}%"
                            binding.tvChartChange.text = changeStr
                            binding.tvChartChange.setTextColor(
                                ContextCompat.getColor(requireContext(), 
                                    if (data.isPositive) com.financewatch.traderapp.R.color.chart_positive
                                    else                 com.financewatch.traderapp.R.color.chart_negative
                                )
                            )

                            binding.lineChartView.setData(data.pricePoints, data.isPositive)
                        }
                        is ChartUiState.Error -> {
                            binding.chartProgress.visibility = View.GONE
                            binding.tvChartError.visibility  = View.VISIBLE
                            binding.lineChartView.visibility = View.INVISIBLE
                            binding.tvChartError.text = state.message
                        }
                    }
                }
            }
        }
    }

    // ─── Observa starea stiri trading ────────────────────────────────────────

    private fun observeTradingNews() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chartViewModel.tradingNewsState.collect { state ->
                    when (state) {
                        is TradingNewsUiState.Loading -> {
                            binding.tradingNewsProgress.visibility = View.VISIBLE
                            binding.tvTradingNewsEmpty.visibility  = View.GONE
                            binding.recyclerTradingNews.visibility = View.GONE
                        }
                        is TradingNewsUiState.Success -> {
                            binding.tradingNewsProgress.visibility = View.GONE
                            binding.tvTradingNewsEmpty.visibility  = View.GONE
                            binding.recyclerTradingNews.visibility = View.VISIBLE

                            // Recreeaza adapter-ul cu label-ul corect al categoriei
                            tradingNewsAdapter = TradingNewsAdapter(
                                categoryLabel = state.categoryLabel,
                                onArticleClick = { article ->
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(article.url)
                                    )
                                    startActivity(intent)
                                }
                            )
                            binding.recyclerTradingNews.adapter = tradingNewsAdapter
                            tradingNewsAdapter.submitList(state.articles)
                        }
                        is TradingNewsUiState.Empty -> {
                            binding.tradingNewsProgress.visibility = View.GONE
                            binding.tvTradingNewsEmpty.visibility  = View.VISIBLE
                            binding.recyclerTradingNews.visibility = View.GONE
                        }
                        is TradingNewsUiState.Error -> {
                            binding.tradingNewsProgress.visibility = View.GONE
                            binding.tvTradingNewsEmpty.visibility  = View.VISIBLE
                            binding.tvTradingNewsEmpty.text = "Eroare la incarcarea stirilor"
                            binding.recyclerTradingNews.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
