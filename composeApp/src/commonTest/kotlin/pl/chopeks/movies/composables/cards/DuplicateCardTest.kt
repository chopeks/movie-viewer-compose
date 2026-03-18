package pl.chopeks.movies.composables.cards

import androidx.compose.ui.test.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import pl.chopeks.core.model.Duplicates
import pl.chopeks.core.model.Video

@OptIn(ExperimentalTestApi::class)
class DuplicateCardTest : StringSpec({
	val video1 = Video(
		id = 1,
		name = "Video 1",
		duration = 1000,
		image = null,
		chips = null
	)
	val video2 = Video(
		id = 2,
		name = "Video 2",
		duration = 2000,
		image = null,
		chips = null
	)
	val duplicates = Duplicates(list = listOf(video1, video2))

	"should display video names" {
		runComposeUiTest {
			setContent {
				DuplicateCard(
					videos = duplicates,
					onClick = {},
					onRemoveClick = {},
					onDumpClick = {},
					onCancelClick = {}
				)
			}
			onNodeWithText("Video 1").assertIsDisplayed()
			onNodeWithText("Video 2").assertIsDisplayed()
		}
	}

	"should call onCancelClick when Not duplicate is clicked" {
		var cancelled = false
		runComposeUiTest {
			setContent {
				DuplicateCard(
					videos = duplicates,
					onClick = {},
					onRemoveClick = {},
					onDumpClick = {},
					onCancelClick = { cancelled = true }
				)
			}
			onNodeWithText("Not duplicate").performClick()
			cancelled shouldBe true
		}
	}

	"should call onRemoveClick when delete button is clicked" {
		var removedVideo: Video? = null

		val singleDup = Duplicates(list = listOf(video1))

		runComposeUiTest {
			setContent {
				DuplicateCard(
					videos = singleDup,
					onClick = {},
					onRemoveClick = { removedVideo = it },
					onDumpClick = {},
					onCancelClick = {}
				)
			}

			onAllNodesWithContentDescription("Delete")[0].performClick()

			removedVideo shouldBe video1
		}
	}

	"should call onDumpClick when dump button is clicked" {
		var dumpedVideo: Video? = null
		val singleDup = Duplicates(list = listOf(video1))

		runComposeUiTest {
			setContent {
				DuplicateCard(
					videos = singleDup,
					onClick = {},
					onRemoveClick = {},
					onDumpClick = { dumpedVideo = it },
					onCancelClick = {}
				)
			}

			onAllNodesWithContentDescription("Delete")[1].performClick()

			dumpedVideo shouldBe video1
		}
	}
})
