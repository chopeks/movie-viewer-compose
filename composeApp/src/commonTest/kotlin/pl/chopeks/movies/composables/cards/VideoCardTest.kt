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
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video
import pl.chopeks.core.model.VideoChips

@OptIn(ExperimentalTestApi::class)
class VideoCardTest : StringSpec({
	fun ComposeUiTest.setConstrainedContent(
		content: @Composable () -> Unit
	) {
		setContent {
			Box(modifier = Modifier.width(200.dp)) {
				content()
			}
		}
	}

	val dummyVideo = Video(
		id = 1,
		name = "My Video",
		duration = 3661000, // 1h 01m 01s
		size = "1GB",
		image = null,
		chips = VideoChips(
			actors = listOf(Actor(1, "Actor Name", null)),
			categories = listOf(Category(1, "Category Name", null))
		)
	)

	"should display video name" {
		runComposeUiTest {
			setConstrainedContent {
				VideoCard(
					video = dummyVideo,
					onClick = {},
					onActorChipClick = {},
					onCategoryChipClick = {},
					onThumbnailClick = {},
					onRemoveClick = {},
					onDumpClick = {}
				)
			}
			onNodeWithText("My Video").assertIsDisplayed()
		}
	}

	"should display formatted duration" {
		runComposeUiTest {
			setConstrainedContent {
				VideoCard(
					video = dummyVideo,
					onClick = {},
					onActorChipClick = {},
					onCategoryChipClick = {},
					onThumbnailClick = {},
					onRemoveClick = {},
					onDumpClick = {}
				)
			}
			onNodeWithText("1:01:01").assertIsDisplayed()
		}
	}

	"should display actor chips" {
		runComposeUiTest {
			setConstrainedContent {
				VideoCard(
					video = dummyVideo,
					onClick = {},
					onActorChipClick = {},
					onCategoryChipClick = {},
					onThumbnailClick = {},
					onRemoveClick = {},
					onDumpClick = {}
				)
			}
			onNodeWithText("Actor Name").assertIsDisplayed()
		}
	}

	"should display category chips" {
		runComposeUiTest {
			setConstrainedContent {
				VideoCard(
					video = dummyVideo,
					onClick = {},
					onActorChipClick = {},
					onCategoryChipClick = {},
					onThumbnailClick = {},
					onRemoveClick = {},
					onDumpClick = {}
				)
			}
			onNodeWithText("Category Name").assertIsDisplayed()
		}
	}

	"menu should expand and show options" {
		runComposeUiTest {
			setConstrainedContent {
				VideoCard(
					video = dummyVideo,
					onClick = {},
					onActorChipClick = {},
					onCategoryChipClick = {},
					onThumbnailClick = {},
					onRemoveClick = {},
					onDumpClick = {}
				)
			}

			onNodeWithContentDescription("Expand button").performClick()

			onNodeWithText("Move to .dump").assertIsDisplayed()
			onNodeWithText("Generate Thumbnail").assertIsDisplayed()
			onNodeWithText("Delete video").assertIsDisplayed()
		}
	}

	"menu options should trigger callbacks" {
		var dumpClicked = false
		var thumbnailClicked = false
		var removeClicked = false

		runComposeUiTest {
			setConstrainedContent {
				VideoCard(
					video = dummyVideo,
					onClick = {},
					onActorChipClick = {},
					onCategoryChipClick = {},
					onThumbnailClick = { thumbnailClicked = true },
					onRemoveClick = { removeClicked = true },
					onDumpClick = { dumpClicked = true }
				)
			}

			// Test Dump
			onNodeWithContentDescription("Expand button").performClick()
			onNodeWithText("Move to .dump").performClick()
			dumpClicked shouldBe true

			// Test Thumbnail
			onNodeWithContentDescription("Expand button").performClick()
			onNodeWithText("Generate Thumbnail").performClick()
			thumbnailClicked shouldBe true

			// Test Remove
			onNodeWithContentDescription("Expand button").performClick()
			onNodeWithText("Delete video").performClick()
			removeClicked shouldBe true
		}
	}
})
