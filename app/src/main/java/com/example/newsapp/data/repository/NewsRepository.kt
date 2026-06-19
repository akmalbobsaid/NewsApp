package com.example.newsapp.data.repository

import com.example.newsapp.BuildConfig
import com.example.newsapp.data.api.RetrofitInstance
import com.example.newsapp.data.model.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

class NewsRepository {

    private val api = RetrofitInstance.api

    suspend fun getTopHeadlines(category: String? = null, page: Int = 1): Result<List<Article>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getTopHeadlines(
                    apiKey = BuildConfig.NEWS_API_KEY,
                    country = "us",
                    category = category,
                    pageSize = 20,
                    page = page
                )
                val articles = response.articles
                    ?.filter { it.title != null && it.title != "[Removed]" }
                    ?: emptyList()
                Result.Success(articles)
            } catch (e: Exception) {
                Result.Error(e.message ?: "An unknown error occurred")
            }
        }

    suspend fun searchNews(query: String, page: Int = 1): Result<List<Article>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.searchNews(
                    apiKey = BuildConfig.NEWS_API_KEY,
                    q = query,
                    sortBy = "publishedAt",
                    pageSize = 20,
                    page = page
                )
                val articles = response.articles
                    ?.filter { it.title != null && it.title != "[Removed]" }
                    ?: emptyList()
                Result.Success(articles)
            } catch (e: Exception) {
                Result.Error(e.message ?: "An unknown error occurred")
            }
        }
}
