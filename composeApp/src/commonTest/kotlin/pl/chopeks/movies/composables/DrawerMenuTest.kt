package pl.chopeks.movies.composables

import androidx.compose.material.DrawerValue
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.Navigator
import io.kotest.core.spec.style.StringSpec

@OptIn(ExperimentalTestApi::class)
class DrawerMenuTest : StringSpec({
	fun ComposeUiTest.setScreenContent(
		content: @Composable () -> Unit
	) {
		setContent {
			Navigator(object : Screen {
				@Composable
				override fun Content() {
					content()
				}

				override val key: ScreenKey
					get() = "test"
			})
		}
	}

	"should display all drawer buttons" {
		runComposeUiTest {
			setScreenContent {
				val drawerState = rememberDrawerState(DrawerValue.Closed)
				val scope = rememberCoroutineScope()
				DrawerMenu(scope = scope, drawerState = drawerState)
			}

			onNodeWithText("Actors").assertExists()
			onNodeWithText("Categories").assertExists()
			onNodeWithText("Videos").assertExists()
			onNodeWithText("Duplicates").assertExists()
			onNodeWithText("Settings").assertExists()
			onNodeWithText("Logs").assertExists()
		}
	}

	"clicking Actors should navigate to ActorsScreen" {
		runComposeUiTest {
			setScreenContent {
				val drawerState = rememberDrawerState(DrawerValue.Closed)
				val scope = rememberCoroutineScope()
				DrawerMenu(scope = scope, drawerState = drawerState)
			}

			onNodeWithText("Actors").performClick()
		}
	}
})