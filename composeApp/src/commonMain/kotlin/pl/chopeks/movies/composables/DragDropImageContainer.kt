package pl.chopeks.movies.composables

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
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
import pl.chopeks.core.model.IntRect
import pl.chopeks.movies.getPlatform

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DragDropImageContainer(
	height: Dp = 300.dp,
	ratio: Float = 1f,
	url: String,
	imageBytes: ByteArray?,
	droppedImageBytes: ByteArray? = null,
	onDroppedImageBytesChanged: (ByteArray) -> Unit,
	onCroppedRectChanged: (IntRect) -> Unit,
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

	var scale by remember { mutableStateOf(1f) }
	var offset by remember { mutableStateOf(Offset.Zero) }

	LaunchedEffect(url, imageBytes, droppedImageBytes) {
		scale = 1f
		offset = Offset.Zero
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
				if (droppedImageBytes == null) {
					AsyncImage(
						model = ImageRequest.Builder(context)
							.data(url ?: imageBytes)
							.size(Size.ORIGINAL)
							.crossfade(true)
							.build(),
						contentDescription = null,
						contentScale = ContentScale.Crop,
						modifier = Modifier.matchParentSize()
					)
				} else {
					BoxWithConstraints(Modifier.matchParentSize().clipToBounds()) {
						var imageRatio by remember { mutableStateOf(1f) }
						var originalWidth by remember { mutableStateOf(0f) }
						var originalHeight by remember { mutableStateOf(0f) }

						val boxWidth = constraints.maxWidth.toFloat()
						val boxHeight = constraints.maxHeight.toFloat()
						val boxRatio = boxWidth / boxHeight

						AsyncImage(
							model = ImageRequest.Builder(context)
								.data(droppedImageBytes)
								.size(Size.ORIGINAL)
								.crossfade(true)
								.build(),
							contentDescription = null,
							contentScale = ContentScale.Fit,
							onSuccess = { state ->
								val size = state.painter.intrinsicSize
								imageRatio = size.width / size.height
								originalWidth = size.width
								originalHeight = size.height
							},
							modifier = Modifier
								.matchParentSize()
								.run {
									pointerInput(scale, imageRatio) {
										awaitPointerEventScope {
											while (true) {
												val event = awaitPointerEvent()

												if (event.type == PointerEventType.Scroll) {
													val delta = event.changes.first().scrollDelta.y
													val zoomFactor = if (delta < 0) 1.1f else 0.9f
													// We use baseScale as our 1.0 starting point
													scale = (scale * zoomFactor).coerceAtLeast(1f)

													offset = clampOffset(offset, scale, boxWidth, boxHeight, imageRatio)
												}

												if (event.type == PointerEventType.Move && event.buttons.isPrimaryPressed) {
													val change = event.changes.first()
													val dragAmount = change.position - change.previousPosition
													offset = clampOffset(offset + dragAmount, scale, boxWidth, boxHeight, imageRatio)
													change.consume()
												}

												onCroppedRectChanged(calculateCropRegion(scale, offset, boxWidth, boxHeight, originalWidth, originalHeight))
											}
										}
									}.graphicsLayer {
										val baseScale = if (boxRatio > imageRatio) boxRatio / imageRatio else imageRatio / boxRatio
										val finalScale = baseScale * scale

										scaleX = finalScale
										scaleY = finalScale
										translationX = offset.x
										translationY = offset.y
									}
								}
						)
					}
				}
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

private fun clampOffset(
	target: Offset,
	userScale: Float,
	boxWidth: Float,
	boxHeight: Float,
	imageRatio: Float
): Offset {
	val boxRatio = boxWidth / boxHeight
	val baseScale = if (boxRatio > imageRatio) {
		boxRatio / imageRatio
	} else {
		imageRatio / boxRatio
	}
	val visualImageWidth: Float
	val visualImageHeight: Float
	if (boxRatio > imageRatio) { // Image was constrained by Height
		visualImageHeight = boxHeight * userScale * baseScale
		visualImageWidth = (boxHeight * imageRatio) * userScale * baseScale
	} else { // Image was constrained by Width
		visualImageWidth = boxWidth * userScale * baseScale
		visualImageHeight = (boxWidth / imageRatio) * userScale * baseScale
	}

	val maxW = ((visualImageWidth - boxWidth) / 2f).coerceAtLeast(0f)
	val maxH = ((visualImageHeight - boxHeight) / 2f).coerceAtLeast(0f)

	return Offset(
		x = target.x.coerceIn(-maxW, maxW),
		y = target.y.coerceIn(-maxH, maxH)
	)
}

private fun calculateCropRegion(
	userScale: Float,
	offset: Offset,
	boxWidth: Float,
	boxHeight: Float,
	imageWidth: Float,
	imageHeight: Float
): IntRect {
	val boxRatio = boxWidth / boxHeight
	val imageRatio = imageWidth / imageHeight

	// This is how much the image was shrunk to fit inside the box initially
	val fitScale = if (imageRatio > boxRatio) {
		boxWidth / imageWidth  // Constrained by width
	} else {
		boxHeight / imageHeight // Constrained by height
	}

	// The TOTAL scale is FitScale * BaseScale (to fill gaps) * UserZoom
	// To fill gaps, scale by the inverse of the smaller dimension's fit
	val gapFillScale = if (imageRatio > boxRatio) {
		boxHeight / (imageHeight * fitScale)
	} else {
		boxWidth / (imageWidth * fitScale)
	}

	val totalVisualScale = fitScale * gapFillScale * userScale

	// Size of the window in Original Pixels
	// This MUST maintain the boxRatio to prevent stretching
	val srcWidth = boxWidth / totalVisualScale
	val srcHeight = boxHeight / totalVisualScale

	// Map UI Offset to Original Pixels
	// One UI unit = (1 / totalVisualScale) original pixels
	val offsetXInOriginal = offset.x / totalVisualScale
	val offsetYInOriginal = offset.y / totalVisualScale

	// Calculate Top-Left (Starting from the center of the image)
	val left = (imageWidth / 2f) - (srcWidth / 2f) - offsetXInOriginal
	val top = (imageHeight / 2f) - (srcHeight / 2f) - offsetYInOriginal

	// Return the IntRect with a "Shift-back" safety to maintain size
	val rect = IntRect(
		left = left.toInt(),
		top = top.toInt(),
		right = (left + srcWidth).toInt(),
		bottom = (top + srcHeight).toInt()
	)

	// Ensure we don't go out of bounds, but keep the width/height stable
	val dx = if (rect.left < 0) -rect.left
	else if (rect.right > imageWidth) (imageWidth - rect.right).toInt()
	else 0
	val dy = if (rect.top < 0) -rect.top
	else if (rect.bottom > imageHeight) (imageHeight - rect.bottom).toInt()
	else 0

	return IntRect(
		left = rect.left + dx,
		top = rect.top + dy,
		right = rect.right + dx,
		bottom = rect.bottom + dy
	)
}
