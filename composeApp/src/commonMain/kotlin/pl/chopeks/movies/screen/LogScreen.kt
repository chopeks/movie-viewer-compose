package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import movieviewer.composeapp.generated.resources.Res
import movieviewer.composeapp.generated.resources.screen_logs
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.compose.rememberInstance
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.utils.AppLogger
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation

class LogScreen : Screen {
	@Composable
	override fun Content() {
		val logs = AppLogger.logLines
		val listState = rememberLazyListState()
		val keyEventManager by rememberInstance<KeyEventManager>()
		val navigator = LocalNavigator.current

		DisposableEffect(keyEventManager, navigator) {
			keyEventManager.setListener { KeyEventNavigation.onKeyEvent(it, navigator) }
			onDispose { keyEventManager.setListener(null) }
		}

		LaunchedEffect(logs.size) {
			if (logs.isNotEmpty()) {
				listState.animateScrollToItem(logs.size - 1)
			}
		}

		ScreenSkeleton(title = stringResource(Res.string.screen_logs)) {
			SelectionContainer {
				LazyColumn(state = listState, modifier = Modifier.padding(8.dp)) {
					items(logs) { line ->
						Text(
							text = line,
							fontSize = 12.sp,
							color = Color.LightGray
						)
					}
				}
			}
		}
	}
}
