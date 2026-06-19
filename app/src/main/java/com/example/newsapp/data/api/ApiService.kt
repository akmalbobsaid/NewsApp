package com.example.newsapp.data.api

import com.example.newsapp.data.model.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("apiKey") apiKey: String,
        @Query("country") country: String,
        @Query("category") category: String?,
        @Query("pageSize") pageSize: Int,
        @Query("page") page: Int
    ): NewsResponse

    @GET("v2/everything")
    suspend fun searchNews(
        @Query("apiKey") apiKey: String,
        @Query("q") q: String,
        @Query("sortBy") sortBy: String,
        @Query("pageSize") pageSize: Int,
        @Query("page") page: Int
    ): NewsResponse
}
