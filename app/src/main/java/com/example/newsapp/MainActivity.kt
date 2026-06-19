package com.example.newsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.newsapp.data.model.Article
import com.example.newsapp.ui.screen.DetailScreen
import com.example.newsapp.ui.screen.HomeScreen
import com.example.newsapp.ui.screen.SearchScreen
import com.example.newsapp.ui.theme.NewsAppTheme
import com.example.newsapp.viewmodel.NewsViewModel

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val DETAIL = "detail"
}

@Composable
fun NewsApp() {
    val viewModel: NewsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedArticle: Article? by remember { mutableStateOf(null) }
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                uiState = uiState,
                onArticleClick = { article ->
                    selectedArticle = article
                    navController.navigate(Routes.DETAIL)
                },
                onSearchClick = { navController.navigate(Routes.SEARCH) }
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                viewModel = viewModel,
                uiState = uiState,
                onArticleClick = { article ->
                    selectedArticle = article
                    navController.navigate(Routes.DETAIL)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DETAIL) {
            DetailScreen(
                article = selectedArticle ?: return@composable,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsAppTheme {
                NewsApp()
            }
        }
    }
}
