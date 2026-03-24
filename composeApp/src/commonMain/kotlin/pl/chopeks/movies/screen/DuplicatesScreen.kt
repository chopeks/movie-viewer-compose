package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import movieviewer.composeapp.generated.resources.Res
import movieviewer.composeapp.generated.resources.button_deduplicate_all
import movieviewer.composeapp.generated.resources.label_left_to_check
import movieviewer.composeapp.generated.resources.screen_duplicates
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import pl.chopeks.movies.composables.ProgressIndicator
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.buttons.GreenTextButton
import pl.chopeks.movies.composables.cards.DuplicateCard
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation
import pl.chopeks.screenmodel.DuplicatesScreenModel
import pl.chopeks.screenmodel.model.UiState

class DuplicatesScreen : Screen {
	@Composable
	override fun Content() {
		val screenModel = rememberScreenModel<DuplicatesScreenModel>()
		val keyEventManager = localDI().direct.instance<KeyEventManager>()
		val navigator = LocalNavigator.current
		keyEventManager.setListener { KeyEventNavigation.onKeyEvent(it, navigator) }

		ScreenSkeleton(
			title = stringResource(Res.string.screen_duplicates),
			rightActions = {
				GreenTextButton(text = stringResource(Res.string.button_deduplicate_all), onClick = {
					screenModel.deduplicate()
				})
				Spacer(Modifier.width(32.dp))
				Text(stringResource(Res.string.label_left_to_check, screenModel.count), color = Color.LightGray)
			}
		) { scope ->
			val state by screenModel.duplicates.collectAsState()
			when (val current = state) {
				is UiState.Success -> Column(modifier = Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
					val chunks = current.data.chunked(2)
					chunks.forEach { chunk ->
						Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
							chunk.forEachIndexed { i, videos ->
								Box(Modifier.fillMaxSize().weight(1f)) {
									DuplicateCard(
										videos,
										onClick = screenModel::play,
										onRemoveClick = screenModel::remove,
										onDumpClick = screenModel::dump,
										onCancelClick = screenModel::cancel
									)
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

				is UiState.Loading -> ProgressIndicator()
				is UiState.Error -> Text("Error: ${current.message}")
			}
		}
	}
}