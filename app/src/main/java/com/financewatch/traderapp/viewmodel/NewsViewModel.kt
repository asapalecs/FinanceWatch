package com.financewatch.traderapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.financewatch.traderapp.data.local.entity.NewsEntity
import com.financewatch.traderapp.repository.NewsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel pentru ecranul principal de stiri.
 * Supravietuieste rotatiei ecranului si pastrzeaza starea UI.
 * Comunica cu Repository si expune date catre Fragment prin StateFlow/SharedFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModel(private val repository: NewsRepository) : ViewModel() {

    // Categoria selectata curent (default: nasdaq)
    private val _selectedCategory = MutableStateFlow("nasdaq")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Starea de incarcare pentru SwipeRefreshLayout
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Eveniment one-shot pentru afisarea erorilor (Snackbar)
    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    /**
     * Lista de stiri reactiva: se actualizeaza automat cand:
     *  - Utilizatorul schimba categoria (flatMapLatest)
     *  - Repository-ul insereaza date noi in Room
     */
    val news: StateFlow<List<NewsEntity>> = _selectedCategory
        .flatMapLatest { category ->
            repository.getCachedNews(category)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Schimba categoria si declanseaza fetch automat prin flatMapLatest.
     */
    fun selectCategory(category: String) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        refresh()
    }

    /**
     * Pull-to-refresh: fetch fortat din API, ignora cache-ul.
     */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.refreshNews(_selectedCategory.value)
            result.onFailure { error ->
                _errorEvent.emit(error.message ?: "Eroare la actualizare")
            }
            _isLoading.value = false
        }
    }

    /**
     * Set-ul de ID-uri ale stirilor salvate — folosit de adapter pentru starea vizuala.
     */
    val savedNewsIds: StateFlow<Set<String>> = repository.getSavedNews()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    /**
     * Toggle save/unsave pentru o stire.
     * Daca e deja salvata → o sterge. Altfel → o salveaza.
     */
    fun toggleSave(news: NewsEntity) {
        viewModelScope.launch {
            if (news.id in savedNewsIds.value) {
                repository.unsaveNews(news.id)
            } else {
                repository.saveNews(news)
            }
        }
    }

    /**
     * Salveaza o stire in bookmarks (backward compat).
     */
    fun saveNews(news: NewsEntity) {
        viewModelScope.launch { repository.saveNews(news) }
    }

    /**
     * Sterge o stire din bookmarks.
     */
    fun unsaveNews(newsId: String) {
        viewModelScope.launch { repository.unsaveNews(newsId) }
    }

    class Factory(private val repository: NewsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NewsViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel necunoscut")
        }
    }
}
