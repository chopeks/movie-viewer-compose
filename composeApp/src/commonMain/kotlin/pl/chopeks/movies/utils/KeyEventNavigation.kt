package pl.chopeks.movies.utils

import androidx.compose.ui.input.key.*
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
        Key.One -> {
          navigator?.replace(ActorsScreen()); return true
        }

        Key.Two -> {
          navigator?.replace(CategoriesScreen()); return true
        }

        Key.Three -> {
          navigator?.replace(VideosScreen()); return true
        }

        Key.Four -> {
          navigator?.replace(DuplicatesScreen()); return true
        }
      }
    }
    return false
  }
}