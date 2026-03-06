package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.launch
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.utils.AppLogger

class LogScreen() : Screen {
  @Composable
  override fun Content() {
    val logs = AppLogger.logLines
    val listState = rememberLazyListState()

    // Auto-scroll to the bottom when a new log arrives
    LaunchedEffect(logs.size) {
      if (logs.isNotEmpty()) {
        listState.animateScrollToItem(logs.size - 1)
      }
    }

    ScreenSkeleton(title = "Logs") { scope ->
      SelectionContainer { // Allows you to copy-paste logs
        LazyColumn(state = listState, modifier = Modifier.padding(8.dp)) {
          items(logs) { line ->
            Text(
              text = line,
              fontSize = 12.sp,
              color = Color.LightGray
            )
          }
        }
      }
    }
  }
}
