package pl.chopeks.movies.utils

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import cafe.adriel.voyager.navigator.Navigator
import pl.chopeks.movies.screen.ActorsScreen
import pl.chopeks.movies.screen.CategoriesScreen
import pl.chopeks.movies.screen.DuplicatesScreen
import pl.chopeks.movies.screen.VideosScreen

object KeyEventNavigation {
  fun onKeyEvent(event: KeyEvent, navigator: Navigator?): Boolean {
    if (event.type != KeyEventType.KeyDown)
      return false
    if (event.isAltPressed) {
      when (event.key) {
        Key(49) -> {
          navigator?.replace(ActorsScreen()); return true
        }

        Key(50) -> {
          navigator?.replace(CategoriesScreen()); return true
        }

        Key(51) -> {
          navigator?.replace(VideosScreen()); return true
        }

        Key(52) -> {
          navigator?.replace(DuplicatesScreen()); return true
        }
      }
    }
    return false
  }
}