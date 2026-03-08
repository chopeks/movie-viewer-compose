package pl.chopeks.movies.internal.screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import pl.chopeks.movies.bestConcurrencyDispatcher
import pl.chopeks.movies.internal.webservice.SettingsAPI
import pl.chopeks.core.model.Path
import pl.chopeks.core.model.Settings

class SettingsScreenModel(
  private val webService: SettingsAPI,
) : ScreenModel {
  var settings by mutableStateOf<Settings?>(null)
  val pathes = mutableStateListOf<Path>()

  fun init() {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      settings = webService.get()
      pathes.clear()
      pathes.addAll(webService.getPathes())
    }
  }

  fun saveSettings(browser: String, moviePlayer: String) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      webService.set(Settings(browser, moviePlayer))
    }
  }

  fun removePath(path: Path) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      webService.remove(path)
    }
  }

  fun addPath(path: String) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      webService.add(path)
    }
  }

  override fun onDispose() {
    super.onDispose()
    webService.close()
  }

}