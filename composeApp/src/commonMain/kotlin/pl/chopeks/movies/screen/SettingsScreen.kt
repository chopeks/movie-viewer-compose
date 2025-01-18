package pl.chopeks.movies.screen

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import pl.chopeks.movies.composables.ScreenSkeleton

class SettingsScreen : Screen {
  @Composable
  override fun Content() {
    ScreenSkeleton(
      title = "Settings"
    ) { scope ->

    }
  }
}