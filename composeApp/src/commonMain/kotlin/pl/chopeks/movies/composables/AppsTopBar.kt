package pl.chopeks.movies.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import movieviewer.composeapp.generated.resources.Res
import movieviewer.composeapp.generated.resources.button_desc_open_drawer
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppsTopBar(
  scope: CoroutineScope,
  drawerState: DrawerState,
  modifier: Modifier = Modifier,
  title: String = "",
  textActions: @Composable RowScope.() -> Unit = {},
  actions: @Composable RowScope.() -> Unit = {},
) {
  TopAppBar(
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        textActions(this)
        Text(title, color = Color.DarkGray)
      }
    },
    backgroundColor = Color.Black,
    modifier = Modifier.fillMaxWidth().then(modifier),
    navigationIcon = {
      IconButton(onClick = {
        scope.launch {
          if (drawerState.isOpen) drawerState.close() else drawerState.open()
        }
      }) {
        Icon(Icons.Filled.Menu, contentDescription = stringResource(Res.string.button_desc_open_drawer), tint = Color.Gray)
      }
    },
    actions = actions
  )
}