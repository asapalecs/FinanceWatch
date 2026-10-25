package com.financewatch.traderapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.financewatch.traderapp.data.local.entity.SavedNewsEntity
import com.financewatch.traderapp.repository.NewsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarksViewModel(private val repository: NewsRepository) : ViewModel() {

    val savedNews: StateFlow<List<SavedNewsEntity>> = repository.getSavedNews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteNews(id: String) {
        viewModelScope.launch { repository.unsaveNews(id) }
    }

    fun restoreNews(entity: SavedNewsEntity) {
        viewModelScope.launch { repository.saveNewsEntity(entity) }
    }

    class Factory(private val repository: NewsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BookmarksViewModel(repository) as T
        }
    }
}
