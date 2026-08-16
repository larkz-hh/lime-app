package xyz.larkzhh.lime.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.search.components.SearchSuggestList
import xyz.larkzhh.lime.ui.search.components.SearchTopBar
import xyz.larkzhh.lime.ui.search.viewmodel.SearchMode
import xyz.larkzhh.lime.ui.search.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // 同步历史记录，
    LaunchedEffect(uiState.mode) {
        if (uiState.mode == SearchMode.Idle) viewModel.reloadHistory()
    }

    BackHandler {
        when (uiState.mode) {
            SearchMode.Result -> viewModel.backToHome()
            else -> navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SearchTopBar(
            query = uiState.query,
            // 结果页面收起键盘
            shouldFocus = uiState.mode != SearchMode.Result,
            onQueryChange = viewModel::onQueryChange,
            onSearch = { viewModel.confirmSearch(uiState.query) },
            onBack = {
                when (uiState.mode) {
                    SearchMode.Result -> viewModel.backToHome()
                    else -> navController.popBackStack()
                }
            },
            onClear = viewModel::clearQuery,
        )
        when (uiState.mode) {
            SearchMode.Idle -> SearchHomeContent(
                history = uiState.history,
                hotWords = uiState.hotWords,
                onKeywordClick = viewModel::confirmSearch,
                onKeywordRemove = viewModel::removeHistory,
                onClearHistory = viewModel::clearHistory,
            )
            SearchMode.Suggest -> SearchSuggestList(
                suggestions = uiState.suggestions,
                query = uiState.query,
                onSuggestionClick = viewModel::confirmSearch,
                onFillQuery = viewModel::onQueryChange,
            )
            SearchMode.Result -> SearchResultContent(
                uiState = uiState,
                onLoadMore = viewModel::loadMore,
                onLikeToggle = viewModel::toggleLike,
                onNoteClick = { noteId ->
                    navController.navigate(Screen.Detail.createRoute(noteId.toString()))
                },
                onSortChange = viewModel::onSortChange,
                onTimeRangeChange = viewModel::onTimeRangeChange,
                onResetFilter = viewModel::resetFilter,
            )
        }
    }
}
