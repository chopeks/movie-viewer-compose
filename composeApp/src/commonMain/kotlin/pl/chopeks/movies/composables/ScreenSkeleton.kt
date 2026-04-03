package pl.chopeks.movies.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pl.chopeks.screenmodel.BaseScreenModel
import pl.chopeks.screenmodel.model.UiEffect

@Composable
fun ScreenSkeleton(
	title: String,
	leftActions: @Composable RowScope.() -> Unit = {},
	rightActions: @Composable RowScope.() -> Unit = {},
	onKeyEvent: (KeyEvent) -> Boolean = { false },
	screenModel: BaseScreenModel?,
	content: @Composable (scope: CoroutineScope) -> Unit
) {
	val drawerState = rememberDrawerState(DrawerValue.Closed)
	val scope = rememberCoroutineScope()
	val scaffoldState = rememberScaffoldState()

	LaunchedEffect(screenModel) {
		screenModel?.effects?.onEach { effect ->
			when (effect) {
				is UiEffect.Toast -> scaffoldState.snackbarHostState.showSnackbar(effect.message)
			}
		}?.launchIn(this)
	}

	Scaffold(
		scaffoldState = scaffoldState,
		topBar = {
			AppsTopBar(
				scope,
				drawerState,
				title = title,
				actions = rightActions,
				textActions = leftActions
			)
		},
		content = { padding ->
			ModalDrawer(
				{ DrawerMenu(scope, drawerState) },
				drawerState = drawerState,
				modifier = Modifier
					.padding(padding)
					.background(Color.Black.copy(alpha = 0.95f))
			) {
				content(scope)
			}
		}
	)
}
