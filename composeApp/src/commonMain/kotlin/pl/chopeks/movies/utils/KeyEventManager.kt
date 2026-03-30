package pl.chopeks.movies.utils

import androidx.compose.ui.input.key.KeyEvent

class KeyEventManager {
  private var callback: ((KeyEvent) -> Boolean)? = null

  fun setListener(callback: ((KeyEvent) -> Boolean)?) {
    this.callback = callback
  }

  fun propagateKeyEvent(event: KeyEvent): Boolean {
    return try {
      callback?.invoke(event) ?: false
    } catch (e: Exception) {
      false
    }
  }
}
