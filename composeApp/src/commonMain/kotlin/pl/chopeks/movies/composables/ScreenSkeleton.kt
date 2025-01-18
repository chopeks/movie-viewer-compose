package pl.chopeks.movies.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.DrawerValue
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import kotlinx.coroutines.CoroutineScope

@Composable
fun ScreenSkeleton(
  title: String,
  textActions: @Composable RowScope.() -> Unit = {},
  actions: @Composable RowScope.() -> Unit = {},
  onKeyEvent: (KeyEvent) -> Boolean = { false },
  content: @Composable (scope: CoroutineScope) -> Unit
) {
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  Scaffold(
    topBar = { AppsTopBar(scope, drawerState, title = title, actions = actions, textActions = textActions) },
    content = {
      ModalDrawer(
        { DrawerMenu(scope, drawerState) },
        drawerState = drawerState,
        modifier = Modifier.background(Color.Black.copy(alpha = 0.95f))
      ) {
        content(scope)
      }
    }
  )
}