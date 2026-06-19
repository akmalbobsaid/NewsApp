package com.example.newsapp.data.model

import com.google.gson.annotations.SerializedName

data class NewsResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("totalResults") val totalResults: Int?,
    @SerializedName("articles") val articles: List<Article>?
)

data class Article(
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("urlToImage") val urlToImage: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("source") val source: Source?,
    @SerializedName("author") val author: String?,
    @SerializedName("content") val content: String?
) {
    val safeTitle: String
        get() = title?.takeIf { it != "[Removed]" } ?: ""

    val safeContent: String
        get() = content?.takeIf { it != "[Removed]" } ?: ""
}

data class Source(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)
