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
import cafe.adriel.voyager.navigator.LocalNavigator
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.cards.DuplicateCard
import pl.chopeks.movies.internal.screenmodel.DuplicatesScreenModel
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation

class DuplicatesScreen : Screen {
  @Composable
  override fun Content() {
    val screenModel = rememberScreenModel<DuplicatesScreenModel>()
    val keyEventManager = localDI().direct.instance<KeyEventManager>()
    val navigator = LocalNavigator.current
    keyEventManager.setListener { KeyEventNavigation.onKeyEvent(it, navigator) }

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