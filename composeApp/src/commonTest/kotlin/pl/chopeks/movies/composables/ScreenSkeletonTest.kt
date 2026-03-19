package pl.chopeks.movies.composables

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.Navigator
import io.kotest.core.spec.style.StringSpec

@OptIn(ExperimentalTestApi::class)
class ScreenSkeletonTest : StringSpec({
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

	"should display title" {
		runComposeUiTest {
			setScreenContent {
				ScreenSkeleton(title = "My Screen") {}
			}
			onNodeWithText("My Screen").assertExists()
		}
	}

	"should display text actions" {
		runComposeUiTest {
			setScreenContent {
				ScreenSkeleton(
					title = "My Screen",
					leftActions = { Text("My Text Action") }
				) {}
			}
			onNodeWithText("My Text Action").assertExists()
		}
	}

	"should display actions" {
		runComposeUiTest {
			setScreenContent {
				ScreenSkeleton(
					title = "My Screen",
					rightActions = { Text("My Action") }
				) {}
			}
			onNodeWithText("My Action").assertExists()
		}
	}

	"should display content" {
		runComposeUiTest {
			setScreenContent {
				ScreenSkeleton(title = "My Screen") {
					Text("My Content")
				}
			}
			onNodeWithText("My Content").assertExists()
		}
	}
})