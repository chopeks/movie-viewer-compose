package pl.chopeks.movies.composables.buttons

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import io.kotest.core.spec.style.StringSpec
import io.kotest.datatest.withData
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe

@OptIn(ExperimentalTestApi::class)
class DrawerButtonTest : StringSpec({
	"should display primary text" {
		runComposeUiTest {
			setContent {
				DrawerButton(text = "Settings", onClick = {})
			}
			onNodeWithTag("drawerButton_title", useUnmergedTree = true)
				.assertExists()
				.assertIsDisplayed()
				.assertTextContains("Settings")
		}
	}

	"title should not exceed 200dp even if string is massive" {
		runComposeUiTest {
			setContent { DrawerButton(text = "A".repeat(1000), onClick = {}) }

			onNodeWithTag("drawerButton_title", true)
				.getBoundsInRoot().width shouldBe 200.dp
		}
	}

	"very long titles should be truncated" {
		runComposeUiTest {
			val longText = "A".repeat(1000)
			setContent {
				Column {
					DrawerButton(text = "Short", onClick = {})
					DrawerButton(text = longText, onClick = {})
				}
			}

			val height = onNodeWithText("Short").getBoundsInRoot().height
			onNodeWithText(longText).getBoundsInRoot().height shouldBe height
		}
	}

	"very long shortcuts should be truncated" {
		runComposeUiTest {
			val longText = "A".repeat(1000)
			setContent {
				Column {
					DrawerButton(text = "Short", shortcut = "AAA", onClick = {})
					DrawerButton(text = "Long", shortcut = longText, onClick = {})
				}
			}

			val height = onNodeWithText("AAA").getBoundsInRoot().height
			onNodeWithText(longText).getBoundsInRoot().height shouldBe height
		}
	}

	"should display shortcut when provided" {
		runComposeUiTest {
			setContent {
				DrawerButton(text = "Save", shortcut = "Ctrl+S", onClick = {})
			}
			onNodeWithTag("drawerButton_shortcut", useUnmergedTree = true)
				.assertTextContains("Ctrl+S")
				.assertIsDisplayed()
		}
	}

	"shortcut should remain fully visible regardless of title length" {
		runComposeUiTest {
			setContent {
				DrawerButton(text = "A".repeat(1000), shortcut = "Ctrl+S", onClick = {})
			}

			onNodeWithTag("drawerButton_shortcut", true)
				.assertIsDisplayed()

			onNodeWithTag("drawerButton_shortcut", true)
				.getBoundsInRoot().width shouldBeGreaterThan 0.dp
		}
	}

	"should NOT display shortcut when null" {
		runComposeUiTest {
			setContent {
				DrawerButton(text = "Save", shortcut = null, onClick = {})
			}
			onNodeWithTag("drawerButton_shortcut", useUnmergedTree = true)
				.assertDoesNotExist()
		}
	}

	"should trigger onClick when row is clicked" {
		var clicked = false
		runComposeUiTest {
			setContent {
				DrawerButton(text = "Click Me", onClick = { clicked = true })
			}

			onNodeWithTag("drawerButton_container").performClick()

			clicked shouldBe true
		}
	}

	withData(
		"Menu" to "M",
		"A very long string, that could technically overlap the bounds of the box, but it shouldn't really hide the shortcut" to "Ctrl+A",
		"Meny" to "A very long shortcut, that could technically overlap the bounds of the box, but it shouldn't really hide the text",
	) { (title, shortcut) ->
		runComposeUiTest {
			setContent {
				DrawerButton(text = title, shortcut = shortcut, onClick = {})
			}

			val titleBounds = onNodeWithTag("drawerButton_title", useUnmergedTree = true)
				.fetchSemanticsNode().boundsInRoot

			val shortcutBounds = onNodeWithTag("drawerButton_shortcut", useUnmergedTree = true)
				.fetchSemanticsNode().boundsInRoot

			shortcutBounds.left shouldBeGreaterThanOrEqualTo titleBounds.right
		}
	}
})