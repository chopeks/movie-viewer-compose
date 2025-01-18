package pl.chopeks.movies.screen

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import pl.chopeks.movies.composables.ScreenSkeleton

class DuplicatesScreen : Screen {
  @Composable
  override fun Content() {
    ScreenSkeleton(
      title = "Duplicates"
    ) { scope ->

    }
  }
}