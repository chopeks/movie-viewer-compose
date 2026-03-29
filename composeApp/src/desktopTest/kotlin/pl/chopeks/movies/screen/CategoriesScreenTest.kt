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
import pl.chopeks.core.model.Category
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.screenmodel.CategoriesScreenModel

@OptIn(ExperimentalTestApi::class)
class CategoriesScreenTest : StringSpec({
	"should display categories list" {
		val state = CategoriesScreenModel.UiState(
			categories = listOf(Category(1, "Drama"), Category(2, "Comedy")),
			isLoading = false
		)
		val screenModel = mockk<CategoriesScreenModel>(relaxed = true)
		every { screenModel.state } returns MutableStateFlow(state)

		val keyEventManager = mockk<KeyEventManager>(relaxed = true)

		val di = DI {
			bind<CategoriesScreenModel>() with instance(screenModel)
			bind<KeyEventManager>() with instance(keyEventManager)
		}

		runComposeUiTest {
			setContent {
				withDI(di) {
					Navigator(CategoriesScreen())
				}
			}

			onNodeWithText("Drama").assertIsDisplayed()
			onNodeWithText("Comedy").assertIsDisplayed()
		}
	}

	"should display progress indicator when loading" {
		val state = CategoriesScreenModel.UiState(isLoading = true)
		val screenModel = mockk<CategoriesScreenModel>(relaxed = true)
		every { screenModel.state } returns MutableStateFlow(state)

		val keyEventManager = mockk<KeyEventManager>(relaxed = true)

		val di = DI {
			bind<CategoriesScreenModel>() with instance(screenModel)
			bind<KeyEventManager>() with instance(keyEventManager)
		}

		runComposeUiTest {
			setContent {
				withDI(di) {
					Navigator(CategoriesScreen())
				}
			}

			onNodeWithTag("ProgressIndicator").assertIsDisplayed()
		}
	}
})