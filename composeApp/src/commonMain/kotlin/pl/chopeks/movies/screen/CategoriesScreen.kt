package pl.chopeks.movies.screen

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import pl.chopeks.movies.composables.ScreenSkeleton

class CategoriesScreen : Screen {
  @Composable
  override fun Content() {
    ScreenSkeleton(
      title = "Categories"
    ) { scope ->

    }
  }
}