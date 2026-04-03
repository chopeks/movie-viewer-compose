package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import org.kodein.di.compose.rememberInstance
import pl.chopeks.movies.composables.ProgressIndicator
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.buttons.GreenTextButton
import pl.chopeks.movies.composables.cards.DuplicateCard
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation
import pl.chopeks.screenmodel.DuplicatesScreenModel
import pl.chopeks.screenmodel.DuplicatesScreenModel.Intent.*

class DuplicatesScreen : Screen {
	@Composable
	override fun Content() {
		val screenModel = rememberScreenModel<DuplicatesScreenModel>()
		val keyEventManager by rememberInstance<KeyEventManager>()
		val navigator = LocalNavigator.current

		DisposableEffect(keyEventManager, navigator) {
			keyEventManager.setListener { KeyEventNavigation.onKeyEvent(it, navigator) }
			onDispose { keyEventManager.setListener(null) }
		}

		val state by screenModel.state.collectAsState()

		ScreenSkeleton(
			title = stringResource(Res.string.screen_duplicates),
			rightActions = {
				GreenTextButton(text = stringResource(Res.string.button_deduplicate_all), onClick = {
					screenModel.handleIntent(DeduplicateAll)
				})
				Spacer(Modifier.width(32.dp))
				Text(
					stringResource(Res.string.label_left_to_check, state.count),
					color = Color.LightGray
				)
			}
		) {
			if (state.isLoading) {
				ProgressIndicator()
			} else if (state.error != null) {
				Text("Error: ${state.error}")
			} else {
				Column(
					modifier = Modifier.fillMaxSize().padding(4.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					val chunks = state.duplicates.chunked(2)
					chunks.forEach { chunk ->
						Row(
							modifier = Modifier.fillMaxWidth().weight(1f),
							horizontalArrangement = Arrangement.spacedBy(8.dp)
						) {
							chunk.forEach { videos ->
								Box(modifier = Modifier.fillMaxSize().weight(1f)) {
									DuplicateCard(
										videos = videos,
										onClick = {
											screenModel.handleIntent(Play(it))
										},
										onRemoveClick = {
											screenModel.handleIntent(Remove(it))
										},
										onDumpClick = {
											screenModel.handleIntent(Dump(it))
										},
										onCancelClick = {
											screenModel.handleIntent(Cancel(it))
										}
									)
								}
							}
							repeat(2 - chunk.size) {
								Spacer(modifier = Modifier.weight(1f))
							}
						}
					}
					repeat(4 - chunks.size) {
						Spacer(modifier = Modifier.weight(1f))
					}
				}
			}
		}
	}
}
