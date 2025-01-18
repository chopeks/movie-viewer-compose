package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import kotlinx.coroutines.launch
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.cards.ActorCard
import pl.chopeks.movies.internal.screenmodel.ActorsScreenModel

class ActorsScreen : Screen {
  @Composable
  override fun Content() {
    val screenModel = rememberScreenModel<ActorsScreenModel>()
    val navigator = LocalNavigator.current

    ScreenSkeleton(
      title = "Actors",
      textActions = {
        TextButton({}) { Text("Add Actors".uppercase(), color = Color.Green.copy(alpha = 0.5f)) }
      }
    ) { scope ->
      Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val items = screenModel.actors.chunked(8)
        items.forEach { chunk ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Spacer(Modifier.fillMaxWidth(0.08f))
            chunk.forEach { actor ->
              Box(modifier = Modifier.weight(1f)) {
                ActorCard(actor) {
                  scope.launch {
                    navigator?.replace(VideosScreen(actor = it))
                  }
                }
              }
            }
            repeat(8 - chunk.size) {
              Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.fillMaxWidth(0.08f))
          }
        }
      }
    }

    LaunchedEffect(Unit) {
      screenModel.getActors()
    }
  }
}