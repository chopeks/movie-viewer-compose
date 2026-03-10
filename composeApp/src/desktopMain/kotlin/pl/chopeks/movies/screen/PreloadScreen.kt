package pl.chopeks.movies.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import pl.chopeks.movies.tasks.DuplicatesSearchTask

class PreloadScreen : Screen {
  @Composable
  override fun Content() {
    val screenModel = rememberScreenModel<PreloadScreenModel>()
    val navigator = LocalNavigator.current
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
      LazyColumn(Modifier.fillMaxSize()) {
        items(screenModel.events) {
          Text(it, color = Color.Gray)
        }
      }
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