package pl.chopeks.movies.screen

import androidx.compose.ui.test.*
import cafe.adriel.voyager.navigator.Navigator
import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.compose.withDI
import org.kodein.di.instance
import pl.chopeks.core.model.Actor
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.screenmodel.ActorsScreenModel

@OptIn(ExperimentalTestApi::class)
class ActorsScreenTest : StringSpec({
	"should display actors list" {
		val state = ActorsScreenModel.UiState(
			actors = listOf(Actor(1, "John Doe"), Actor(2, "Jane Smith")),
			isLoading = false
		)
		val screenModel = mockk<ActorsScreenModel>(relaxed = true)
		every { screenModel.state } returns MutableStateFlow(state)

		val keyEventManager = mockk<KeyEventManager>(relaxed = true)

		val di = DI {
			bind<ActorsScreenModel>() with instance(screenModel)
			bind<KeyEventManager>() with instance(keyEventManager)
		}

		runComposeUiTest {
			setContent {
				withDI(di) {
					Navigator(ActorsScreen())
				}
			}

			onNodeWithText("John Doe").assertIsDisplayed()
			onNodeWithText("Jane Smith").assertIsDisplayed()
		}
	}

	"should display progress indicator when loading" {
		val state = ActorsScreenModel.UiState(isLoading = true)
		val screenModel = mockk<ActorsScreenModel>(relaxed = true)
		every { screenModel.state } returns MutableStateFlow(state)

		val keyEventManager = mockk<KeyEventManager>(relaxed = true)

		val di = DI {
			bind<ActorsScreenModel>() with instance(screenModel)
			bind<KeyEventManager>() with instance(keyEventManager)
		}

		runComposeUiTest {
			setContent {
				withDI(di) {
					Navigator(ActorsScreen())
				}
			}

			onNodeWithTag("ProgressIndicator").assertIsDisplayed()
		}
	}
})