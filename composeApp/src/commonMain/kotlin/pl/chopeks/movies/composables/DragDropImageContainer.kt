package pl.chopeks.movies.composables

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import movieviewer.composeapp.generated.resources.Res
import movieviewer.composeapp.generated.resources.drag_drop_image
import org.jetbrains.compose.resources.stringResource
import pl.chopeks.movies.getPlatform

@Composable
fun DragDropImageContainer(
	height: Dp = 300.dp,
	ratio: Float = 1f,
	url: String,
	imageBytes: ByteArray?,
	droppedImageBytes: ByteArray? = null,
	onDroppedImageBytesChanged: (ByteArray) -> Unit,
) {
	val platform = remember { getPlatform() }
	val context = LocalPlatformContext.current
	val dragAndDropTarget = remember {
		object : DragAndDropTarget {
			override fun onDrop(event: DragAndDropEvent): Boolean {
				return platform.getDragAndDropFiles(event) { list ->
					list.firstOrNull()?.let {
						onDroppedImageBytesChanged(it)
					}
				}
			}
		}
	}

	val url = url.ifBlank { null }
	Box(Modifier.fillMaxWidth()) {
		Box(
			Modifier.height(height)
				.align(Alignment.Center)
				.aspectRatio(ratio)
				.clip(RoundedCornerShape(8.dp))
				.drawBehind {
					val stroke = Stroke(
						width = 1.dp.toPx(),
						pathEffect = PathEffect.dashPathEffect(
							intervals = floatArrayOf(5.dp.toPx(), 3.dp.toPx()),
							phase = 0f
						)
					)
					drawRoundRect(
						color = Color.Gray,
						style = stroke,
						cornerRadius = CornerRadius(8.dp.toPx())
					)
				}
				.dragAndDropTarget(
					shouldStartDragAndDrop = { true },
					target = dragAndDropTarget
				)
		) {
			if (droppedImageBytes != null || url != null || imageBytes != null) {
				AsyncImage(
					model = ImageRequest.Builder(context)
						.data(droppedImageBytes ?: url ?: imageBytes)
						.size(Size.ORIGINAL)
						.crossfade(true)
						.build(),
					contentDescription = null,
					contentScale = ContentScale.Crop,
					modifier = Modifier.matchParentSize()
				)
			} else {
				Text(
					modifier = Modifier.fillMaxWidth().align(Alignment.Center),
					text = stringResource(Res.string.drag_drop_image),
					color = Color.Gray,
					textAlign = TextAlign.Center,
				)
			}
		}
	}
}