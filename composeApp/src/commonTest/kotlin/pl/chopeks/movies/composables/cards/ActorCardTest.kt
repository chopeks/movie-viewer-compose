package pl.chopeks.movies.composables.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import pl.chopeks.core.model.Actor

@OptIn(ExperimentalTestApi::class)
class ActorCardTest : StringSpec({
	fun ComposeUiTest.setConstrainedContent(
		content: @Composable () -> Unit
	) {
		setContent {
			Box(modifier = Modifier.width(200.dp)) {
				content()
			}
		}
	}

	"should display actor name" {
		val actor = Actor(id = 1, name = "John Doe", imageBytes = null)
		runComposeUiTest {
			setConstrainedContent {
				ActorCard(actor = actor, onClick = {}, onEditClick = {})
			}
			onNodeWithText("John Doe").assertIsDisplayed()
		}
	}

	"should call onClick when card is clicked" {
		var clickedActor: Actor? = null
		val actor = Actor(id = 1, name = "John Doe", imageBytes = null)
		runComposeUiTest {
			setConstrainedContent {
				ActorCard(actor = actor, onClick = { clickedActor = it }, onEditClick = {})
			}
			onRoot().performClick()
			clickedActor shouldBe actor
		}
	}

	"should call onEditClick when edit button is clicked" {
		var editedActor: Actor? = null
		val actor = Actor(id = 1, name = "John Doe", imageBytes = null)
		runComposeUiTest {
			setConstrainedContent {
				ActorCard(actor = actor, onClick = {}, onEditClick = { editedActor = it })
			}
//			onRoot(useUnmergedTree = true).printToLog("DEBUG_TREE")

			onNodeWithTag("editButton", useUnmergedTree = true)
				.assertExists()
				.performClick()

			editedActor shouldBe actor
		}
	}

	"should display image" {
		val actor = Actor(id = 1, name = "John Doe", imageBytes = null)
		runComposeUiTest {
			setConstrainedContent {
				ActorCard(actor = actor, onClick = {}, onEditClick = {})
			}
			onNodeWithContentDescription("John Doe").assertExists()
		}
	}
})
