package pl.chopeks.movies.composables

import androidx.compose.material.DrawerValue
import androidx.compose.material.Text
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.*
import io.kotest.core.spec.style.StringSpec

@OptIn(ExperimentalTestApi::class)
class AppsTopBarTest : StringSpec({
	"should display title" {
		runComposeUiTest {
			setContent {
				val drawerState = rememberDrawerState(DrawerValue.Closed)
				val scope = rememberCoroutineScope()
				AppsTopBar(scope = scope, drawerState = drawerState, title = "My App Title")
			}
			onNodeWithText("My App Title").assertIsDisplayed()
		}
	}

	"should display navigation icon" {
		runComposeUiTest {
			setContent {
				val drawerState = rememberDrawerState(DrawerValue.Closed)
				val scope = rememberCoroutineScope()
				AppsTopBar(scope = scope, drawerState = drawerState)
			}
			onNodeWithContentDescription("Open Drawer").assertIsDisplayed()
		}
	}

	"should toggle drawer when navigation icon is clicked" {
		runComposeUiTest {
			setContent {
				val drawerState = rememberDrawerState(DrawerValue.Closed)
				val scope = rememberCoroutineScope()
				AppsTopBar(scope = scope, drawerState = drawerState)
			}
			onNodeWithContentDescription("Open Drawer").assertHasClickAction()
		}
	}

	"should display text actions" {
		runComposeUiTest {
			setContent {
				val drawerState = rememberDrawerState(DrawerValue.Closed)
				val scope = rememberCoroutineScope()
				AppsTopBar(
					scope = scope,
					drawerState = drawerState,
					textActions = { Text("Text Action") }
				)
			}
			onNodeWithText("Text Action").assertIsDisplayed()
		}
	}

	"should display actions" {
		runComposeUiTest {
			setContent {
				val drawerState = rememberDrawerState(DrawerValue.Closed)
				val scope = rememberCoroutineScope()
				AppsTopBar(
					scope = scope,
					drawerState = drawerState,
					actions = { Text("Action Item") }
				)
			}
			onNodeWithText("Action Item").assertIsDisplayed()
		}
	}
})
