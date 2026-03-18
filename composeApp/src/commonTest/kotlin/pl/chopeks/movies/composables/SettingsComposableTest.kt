package pl.chopeks.movies.composables

import androidx.compose.ui.test.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import pl.chopeks.core.model.Path

@OptIn(ExperimentalTestApi::class)
class SettingsComposableTest : StringSpec({
	"SettingsHeaderText should display text" {
		runComposeUiTest {
			setContent {
				SettingsHeaderText("My Header")
			}

			onNodeWithText("My Header").assertExists()
		}
	}

	"SettingsDirectory should display path" {
		runComposeUiTest {
			setContent {
				SettingsDirectory(Path(path = "my/path", count = 0), onRemove = {})
			}

			onNodeWithText("my/path").assertExists()
		}
	}

	"SettingsDirectory should call onRemove when button is clicked" {
		var removedPath: Path? = null
		val path = Path(path = "my/path", count = 0)
		runComposeUiTest {
			setContent {
				SettingsDirectory(path, onRemove = { removedPath = it })
			}

			onNodeWithContentDescription("remove").performClick()

			removedPath shouldBe path
		}
	}
})
