package pl.chopeks.movies.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator

class PreloadScreen : Screen {
  @Composable
  override fun Content() {
    val screenModel = rememberScreenModel<PreloadScreenModel>()
    val navigator = LocalNavigator.current
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

    }

    LaunchedEffect(Unit) {
      screenModel.init()
    }
    LaunchedEffect(screenModel.isDone) {
      if (screenModel.isDone) {
        navigator?.replace(ActorsScreen())
      }
    }
  }
}