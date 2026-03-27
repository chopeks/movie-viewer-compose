package pl.chopeks.movies.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import movieviewer.composeapp.generated.resources.Res
import movieviewer.composeapp.generated.resources.screen_encoder
import org.jetbrains.compose.resources.stringResource
import pl.chopeks.movies.composables.ProgressIndicator
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.screenmodel.EncoderScreenModel
import pl.chopeks.screenmodel.model.UiState

class EncoderScreen : Screen {
	@Composable
	override fun Content() {
		val screenModel = rememberScreenModel<EncoderScreenModel>()
		val state by screenModel.uiState.collectAsState()

		ScreenSkeleton(
			title = stringResource(Res.string.screen_encoder)
		) { scope ->
			when (val current = state) {
				is UiState.Loading -> ProgressIndicator()
				is UiState.Error -> Text("Error: ${current.message}")
				is UiState.Success -> {
					LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth()) {
						items(current.data.encoder, { it.fileName }) { item ->
							Column(
								modifier = Modifier
									.fillMaxWidth()
									.padding(4.dp)
									.background(MaterialTheme.colors.surface, RoundedCornerShape(8.dp))
									.border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
									.padding(16.dp)
							) {
								Row(
									modifier = Modifier.fillMaxWidth(),
									horizontalArrangement = Arrangement.SpaceBetween
								) {
									Text(
										text = item.fileName,
										style = MaterialTheme.typography.subtitle1,
										maxLines = 1,
										overflow = TextOverflow.Ellipsis,
										modifier = Modifier.weight(1f)
									)
									Text(
										text = "${(item.progress * 100).toInt()}%",
										style = MaterialTheme.typography.body2,
										color = MaterialTheme.colors.primary
									)
								}

								Spacer(modifier = Modifier.height(12.dp))

								val animatedProgress by animateFloatAsState(
									targetValue = item.progress,
									animationSpec = tween(
										durationMillis = 250,
										easing = LinearEasing
									),
									label = "progressAnimation"
								)

								LinearProgressIndicator(
									progress = animatedProgress,
									modifier = Modifier.fillMaxWidth().height(8.dp),
									backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
									strokeCap = StrokeCap.Round
								)
							}
						}
					}

					LaunchedEffect(screenModel) {
						screenModel.startEncoder()
					}
				}
			}
		}
	}
}