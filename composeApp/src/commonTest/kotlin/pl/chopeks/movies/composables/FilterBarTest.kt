package pl.chopeks.movies.composables

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe


@OptIn(ExperimentalTestApi::class)
class FilterBarTest : StringSpec({
	"should display label regardless the query and should be smaller when query not empty" {
		runComposeUiTest {
			var text by mutableStateOf("")
			setContent {
				FilterBar(query = text, onQueryChange = { text = it })
			}

			val preHeight = onNodeWithText("Filter")
				.assertExists()
				.assertIsDisplayed()
				.fetchSemanticsNode()
				.boundsInRoot.height

			text = "Query"

			onNodeWithText("Filter")
				.assertExists()
				.assertIsDisplayed()
				.fetchSemanticsNode()
				.boundsInRoot.height < preHeight
		}
	}

	"should display search icon when query empty, and hide it when not empty" {
		runComposeUiTest {
			var text by mutableStateOf("")
			setContent {
				FilterBar(query = text, onQueryChange = { text = it })
			}

			onNodeWithContentDescription("Search")
				.assertExists()
				.assertIsDisplayed()

			text = "Query"

			onNodeWithContentDescription("Search")
				.assertDoesNotExist()
		}
	}

	"should display delete icon when query not empty, and hide when empty" {
		runComposeUiTest {
			var text by mutableStateOf("")
			setContent {
				FilterBar(query = text, onQueryChange = { text = it })
			}

			onNodeWithTag("deleteButton")
				.assertDoesNotExist()

			text = "Query"

			onNodeWithTag("deleteButton")
				.assertExists()
				.assertIsDisplayed()
		}
	}

	"should display new query if changed from outside" {
		runComposeUiTest {
			var text by mutableStateOf("")
			setContent {
				FilterBar(query = text, onQueryChange = { text = it })
			}

			onNodeWithTag("textField")
				.fetchSemanticsNode()
				.config[SemanticsProperties.EditableText].text shouldBe ""

			text = "Query"

			onNodeWithTag("textField")
				.fetchSemanticsNode()
				.config[SemanticsProperties.EditableText].text shouldBe "Query"
		}
	}

	"should display set query empty when delete icon clicked" {
		runComposeUiTest {
			var text by mutableStateOf("Query")
			setContent {
				FilterBar(query = text, onQueryChange = { text = it })
			}
			onNodeWithTag("deleteButton")
				.assertExists()
				.assertIsDisplayed()
				.performClick()

			onNodeWithTag("textField")
				.fetchSemanticsNode()
				.config[SemanticsProperties.EditableText].text shouldBe ""
		}
	}
})