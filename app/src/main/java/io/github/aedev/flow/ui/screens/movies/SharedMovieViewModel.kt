package io.github.aedev.flow.ui.screens.movies

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * One [MovieViewModel] per activity, shared across the Movies tab, detail screen,
 * and movie search.
 *
 * This preserves catalog state, prevents redundant network requests, avoids ViewModel
 * recreation lag when navigating, and guarantees butter-smooth 60fps transitions.
 */
@Composable
fun sharedMovieViewModel(): MovieViewModel {
    val activity = LocalContext.current as? ComponentActivity
    return if (activity != null) hiltViewModel(activity) else hiltViewModel()
}
