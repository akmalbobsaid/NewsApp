package com.example.newsapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.newsapp.data.model.Article
import com.example.newsapp.ui.component.NewsCard
import com.example.newsapp.ui.component.NewsCardFeatured
import com.example.newsapp.ui.component.NewsCardSkeleton
import com.example.newsapp.viewmodel.NewsUiState
import com.example.newsapp.viewmodel.NewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NewsViewModel,
    uiState: NewsUiState,
    onArticleClick: (Article) -> Unit,
    onSearchClick: () -> Unit
) {
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisible >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingMore && !uiState.isLoadingHeadlines) {
            viewModel.loadTopHeadlines(loadMore = true)
        }
    }

    val featuredArticles = remember(uiState.headlines) { uiState.headlines.take(3) }
    val latestArticles = remember(uiState.headlines) { uiState.headlines.drop(3) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("News") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            isRefreshing = uiState.isLoadingHeadlines && uiState.headlines.isNotEmpty(),
            onRefresh = { viewModel.refreshHeadlines() },
            state = pullState
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Category chips — always visible so users can switch while loading
                item(key = "categories") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.categories) { category ->
                            FilterChip(
                                selected = category == uiState.selectedCategory,
                                onClick = { viewModel.selectCategory(category) },
                                label = {
                                    Text(
                                        text = category.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            )
                        }
                    }
                }

                when {
                    uiState.isLoadingHeadlines && uiState.headlines.isEmpty() -> {
                        items(5, key = { "skeleton_$it" }) {
                            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                NewsCardSkeleton()
                            }
                        }
                    }

                    uiState.headlinesError != null && uiState.headlines.isEmpty() -> {
                        val errorMessage = uiState.headlinesError
                        item(key = "error") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = errorMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                    Button(onClick = { viewModel.loadTopHeadlines() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        if (featuredArticles.isNotEmpty()) {
                            item(key = "featured_title") {
                                Text(
                                    text = "Featured",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        top = 8.dp,
                                        bottom = 4.dp
                                    )
                                )
                            }
                            item(key = "featured_row") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        featuredArticles,
                                        key = { it.url ?: it.hashCode().toString() }
                                    ) { article ->
                                        NewsCardFeatured(
                                            article = article,
                                            onClick = { onArticleClick(article) }
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "latest_title") {
                            Text(
                                text = "Latest News",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 12.dp,
                                    bottom = 4.dp
                                )
                            )
                        }

                        items(
                            latestArticles,
                            key = { it.url ?: it.hashCode().toString() }
                        ) { article ->
                            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                NewsCard(
                                    article = article,
                                    onClick = { onArticleClick(article) }
                                )
                            }
                        }

                        if (uiState.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
