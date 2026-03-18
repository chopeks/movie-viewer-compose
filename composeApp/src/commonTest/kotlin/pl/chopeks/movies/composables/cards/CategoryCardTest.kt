package pl.chopeks.movies.composables.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import pl.chopeks.core.model.Category

@OptIn(ExperimentalTestApi::class)
class CategoryCardTest : StringSpec({
	fun ComposeUiTest.setConstrainedContent(
		content: @Composable () -> Unit
	) {
		setContent {
			Box(modifier = Modifier.width(200.dp)) {
				content()
			}
		}
	}

	"should display category name" {
		val category = Category(id = 1, name = "My Category", image = null)
		runComposeUiTest {
			setConstrainedContent {
				CategoryCard(category = category, onClick = {}, onEditClick = {})
			}
			onNodeWithText("My Category").assertIsDisplayed()
		}
	}

	"should call onClick when card is clicked" {
		var clickedCategory: Category? = null
		val category = Category(id = 1, name = "My Category", image = null)
		runComposeUiTest {
			setConstrainedContent {
				CategoryCard(category = category, onClick = { clickedCategory = it }, onEditClick = {})
			}
			onRoot().performClick()
			clickedCategory shouldBe category
		}
	}

	"should call onEditClick when edit button is clicked" {
		var editedCategory: Category? = null
		val category = Category(id = 1, name = "My Category", image = null)
		runComposeUiTest {
			setConstrainedContent {
				CategoryCard(category = category, onClick = {}, onEditClick = { editedCategory = it })
			}
			onNodeWithTag("editButton", useUnmergedTree = true).performClick()
			editedCategory shouldBe category
		}
	}

	"should display image content description" {
		val category = Category(id = 1, name = "My Category", image = null)
		runComposeUiTest {
			setConstrainedContent {
				CategoryCard(category = category, onClick = {}, onEditClick = {})
			}
			onNodeWithContentDescription("My Category").assertExists()
		}
	}
})
