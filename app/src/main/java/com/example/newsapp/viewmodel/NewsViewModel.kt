package com.example.newsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.model.Article
import com.example.newsapp.data.repository.NewsRepository
import com.example.newsapp.data.repository.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
    val headlines: List<Article> = emptyList(),
    val searchResults: List<Article> = emptyList(),
    val isLoadingHeadlines: Boolean = false,
    val isLoadingSearch: Boolean = false,
    val isLoadingMore: Boolean = false,
    val headlinesError: String? = null,
    val searchError: String? = null,
    val selectedCategory: String = "general",
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val hasMoreHeadlines: Boolean = true
)

private const val PAGE_SIZE = 20

class NewsViewModel : ViewModel() {

    val categories = listOf(
        "general", "business", "entertainment", "health", "science", "sports", "technology"
    )

    private val repository = NewsRepository()

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private var headlinesJob: Job? = null
    private var searchJob: Job? = null
    private var searchPage = 1
    private var hasMoreSearch = true

    init {
        loadTopHeadlines()
    }

    fun loadTopHeadlines(loadMore: Boolean = false) {
        val state = _uiState.value
        if (loadMore && (!state.hasMoreHeadlines || state.isLoadingMore)) return
        if (!loadMore && state.isLoadingHeadlines) return

        headlinesJob?.cancel()
        headlinesJob = viewModelScope.launch {
            val currentState = _uiState.value
            val page = if (loadMore) currentState.currentPage + 1 else currentState.currentPage

            _uiState.update {
                if (loadMore) it.copy(isLoadingMore = true)
                else it.copy(isLoadingHeadlines = true, headlinesError = null)
            }

            when (val result = repository.getTopHeadlines(
                category = currentState.selectedCategory,
                page = page
            )) {
                is Result.Success -> {
                    val articles = result.data
                    _uiState.update {
                        it.copy(
                            headlines = if (loadMore) it.headlines + articles else articles,
                            isLoadingHeadlines = false,
                            isLoadingMore = false,
                            headlinesError = null,
                            currentPage = page,
                            hasMoreHeadlines = articles.size >= PAGE_SIZE
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingHeadlines = false,
                            isLoadingMore = false,
                            headlinesError = result.message
                        )
                    }
                }
            }
        }
    }

    fun refreshHeadlines() {
        headlinesJob?.cancel()
        _uiState.update {
            it.copy(
                currentPage = 1,
                hasMoreHeadlines = true,
                isLoadingHeadlines = false,
                isLoadingMore = false
            )
        }
        loadTopHeadlines()
    }

    fun selectCategory(category: String) {
        if (category == _uiState.value.selectedCategory) return
        headlinesJob?.cancel()
        _uiState.update {
            it.copy(
                selectedCategory = category,
                headlines = emptyList(),
                currentPage = 1,
                hasMoreHeadlines = true,
                headlinesError = null,
                isLoadingHeadlines = false,
                isLoadingMore = false
            )
        }
        loadTopHeadlines()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(searchResults = emptyList(), searchError = null, isLoadingSearch = false)
            }
            searchPage = 1
            hasMoreSearch = true
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            searchPage = 1
            hasMoreSearch = true
            performSearch(query = query, page = searchPage, appendResults = false)
        }
    }

    fun loadMoreSearchResults() {
        val state = _uiState.value
        if (!hasMoreSearch || state.isLoadingSearch || state.isLoadingMore || state.searchQuery.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            searchPage++
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = repository.searchNews(query = state.searchQuery, page = searchPage)) {
                is Result.Success -> {
                    hasMoreSearch = result.data.size >= PAGE_SIZE
                    _uiState.update {
                        it.copy(
                            searchResults = it.searchResults + result.data,
                            isLoadingMore = false,
                            searchError = null
                        )
                    }
                }
                is Result.Error -> {
                    searchPage--
                    _uiState.update { it.copy(isLoadingMore = false, searchError = result.message) }
                }
            }
        }
    }

    private suspend fun performSearch(query: String, page: Int, appendResults: Boolean) {
        _uiState.update { it.copy(isLoadingSearch = true, searchError = null) }
        when (val result = repository.searchNews(query = query, page = page)) {
            is Result.Success -> {
                hasMoreSearch = result.data.size >= PAGE_SIZE
                _uiState.update {
                    it.copy(
                        searchResults = if (appendResults) it.searchResults + result.data else result.data,
                        isLoadingSearch = false,
                        searchError = null
                    )
                }
            }
            is Result.Error -> {
                _uiState.update { it.copy(isLoadingSearch = false, searchError = result.message) }
            }
        }
    }
}
