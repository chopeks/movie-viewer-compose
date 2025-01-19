package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.cards.DuplicateCard
import pl.chopeks.movies.internal.screenmodel.DuplicatesScreenModel

class DuplicatesScreen : Screen {
  @Composable
  override fun Content() {
    val screenModel = rememberScreenModel<DuplicatesScreenModel>()
    ScreenSkeleton(
      title = "Duplicates",
      actions = {
        Text("Left to check ${screenModel.count}", color = Color.LightGray)
      }
    ) { scope ->
      Column(modifier = Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val chunks = screenModel.duplicates.chunked(2)
        chunks.forEach { chunk ->
          Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chunk.forEachIndexed { i, item ->
              Box(Modifier.fillMaxSize().weight(1f)) {
                DuplicateCard(item, onClick = {
                  screenModel.play(it)
                }, onRemoveClick = {
                  screenModel.remove(it)
                }, onCancelClick = {
                  screenModel.cancel(item)
                })
              }
              repeat(2 - chunk.size) {
                Spacer(Modifier.weight(1f))
              }
            }
          }
        }
        repeat(4 - chunks.size) {
          Spacer(Modifier.weight(1f))
        }
      }

      LaunchedEffect(screenModel) {
        screenModel.getDuplicates()
      }
    }
  }
}