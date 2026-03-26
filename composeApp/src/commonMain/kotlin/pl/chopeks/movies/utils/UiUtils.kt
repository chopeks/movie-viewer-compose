package pl.chopeks.movies.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.StateFlow
import pl.chopeks.screenmodel.model.UiState

@Composable
fun <T, R> StateFlow<UiState<T>>.collectAsSuccessState(
	initial: R,
	selector: (T) -> R?
): R {
	val state by this.collectAsState()
	return (state as? UiState.Success<T>)?.data?.let(selector) ?: initial
}