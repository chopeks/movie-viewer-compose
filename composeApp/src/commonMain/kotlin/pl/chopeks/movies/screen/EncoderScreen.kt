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
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import movieviewer.composeapp.generated.resources.Res
import movieviewer.composeapp.generated.resources.screen_encoder
import org.jetbrains.compose.resources.stringResource
import pl.chopeks.core.model.EncodeStatus
import pl.chopeks.movies.composables.ProgressIndicator
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.screenmodel.EncoderScreenModel
import pl.chopeks.screenmodel.model.UiState

class EncoderScreen : Screen {
	@Composable
	override fun Content() {
		val screenModel = rememberScreenModel<EncoderScreenModel>()
		val state by screenModel.uiState.collectAsState()
		val successGreen = Color(0xFF4CAF50)

		ScreenSkeleton(
			title = stringResource(Res.string.screen_encoder)
		) { scope ->
			when (val current = state) {
				is UiState.Loading -> ProgressIndicator()
				is UiState.Error -> Text("Error: ${current.message}")
				is UiState.Success -> {
					LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth()) {
						items(current.data.encoder.toList(), { it.first }) { pair ->
							val item = pair.second

							val color = when (item) {
								is EncodeStatus.Processing -> MaterialTheme.colors.primary
								is EncodeStatus.Finished,
								is EncodeStatus.FinishedAndRemoved -> successGreen

								is EncodeStatus.Error -> MaterialTheme.colors.error
								is EncodeStatus.Waiting -> Color.Gray
							}

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
										text = pair.first,
										style = MaterialTheme.typography.subtitle1,
										maxLines = 1,
										overflow = TextOverflow.Ellipsis,
										modifier = Modifier.weight(1f)
									)

									when (item) {
										is EncodeStatus.FinishedAndRemoved ->
											Text("Done", style = MaterialTheme.typography.caption)

										is EncodeStatus.Finished ->
											Text("100%", style = MaterialTheme.typography.caption)

										is EncodeStatus.Processing ->
											Text("${(item.progress * 100).toInt()}%", color = color, style = MaterialTheme.typography.caption)

										is EncodeStatus.Waiting ->
											Text("In Queue", style = MaterialTheme.typography.caption)

										is EncodeStatus.Error ->
											Icon(Icons.Default.Warning, "Error", tint = color)
									}
								}

								Spacer(modifier = Modifier.height(12.dp))

								val animatedProgress by animateFloatAsState(
									targetValue = when (item) {
										is EncodeStatus.Waiting -> 0f
										is EncodeStatus.Processing -> item.progress
										is EncodeStatus.FinishedAndRemoved,
										is EncodeStatus.Finished -> 1f
										is EncodeStatus.Error -> 0f
									},
									animationSpec = tween(durationMillis = 250, easing = LinearEasing),
									label = "progressAnimation"
								)

								LinearProgressIndicator(
									progress = animatedProgress,
									modifier = Modifier.fillMaxWidth().height(8.dp),
									color = when (item) {
										is EncodeStatus.Finished -> successGreen
										is EncodeStatus.Processing -> MaterialTheme.colors.primary
										else -> successGreen.copy(alpha = 0.5f)
									},
									backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
									strokeCap = StrokeCap.Round
								)
//									item.eta?.let {
//										Text(it.toRemainingLabel(), style = MaterialTheme.typography.caption)
//									}
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