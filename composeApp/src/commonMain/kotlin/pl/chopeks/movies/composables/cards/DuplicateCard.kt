package pl.chopeks.movies.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Size
import movieviewer.composeapp.generated.resources.Res
import movieviewer.composeapp.generated.resources.button_desc_delete
import org.jetbrains.compose.resources.stringResource
import pl.chopeks.core.model.Duplicates
import pl.chopeks.core.model.Video
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private fun convertMillisToDuration(millis: Long?): String {
	if (millis == null)
		return ""

	val hours = millis / 3600000
	val minutes = (millis % 3600000) / 60000
	val seconds = (millis % 60000) / 1000

	return buildString {
		if (hours > 0) append("$hours:")
		append("${if (minutes < 10) "0$minutes" else minutes}:")
		append("${if (seconds < 10) "0$seconds" else seconds}")
	}
}

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun DuplicateCard(
	videos: Duplicates,
	onClick: (Video) -> Unit,
	onRemoveClick: (Video) -> Unit,
	onDumpClick: (Video) -> Unit,
	onCancelClick: () -> Unit
) {
	val context = LocalPlatformContext.current
	Card(Modifier.fillMaxSize(), backgroundColor = Color.DarkGray.copy(alpha = 0.5f), elevation = 0.dp) {
		Row(Modifier.fillMaxWidth()) {
			videos.list.forEach { video ->
				Box(Modifier.fillMaxHeight().weight(1f)) {
					AsyncImage(
						model = video.image.let {
							ImageRequest.Builder(context)
								.data(it?.let(Base64.Mime::decode))
								.size(Size.ORIGINAL)
								.build()
						},
						contentDescription = null,
						contentScale = ContentScale.Crop,
						modifier = Modifier.fillMaxWidth().clickable { onClick(video) }
					)
					Row(
						modifier = Modifier.fillMaxWidth()
							.align(Alignment.BottomCenter)
							.background(Color.Black.copy(alpha = 0.6f)),
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(video.name, fontSize = TextUnit(14f, TextUnitType.Sp), color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
					}

					Text(
						convertMillisToDuration(video.duration),
						fontSize = TextUnit(12f, TextUnitType.Sp),
						color = Color.LightGray,
						modifier = Modifier.background(Color.Black.copy(0.5f), shape = RoundedCornerShape(bottomEnd = 4.dp))
							.padding(horizontal = 4.dp).padding(bottom = 2.dp)
							.offset(y = (-2).dp)
					)

					Text(
						video.size ?: "",
						fontSize = TextUnit(12f, TextUnitType.Sp),
						color = Color.LightGray,
						modifier = Modifier.background(Color.Black.copy(0.5f), shape = RoundedCornerShape(bottomStart = 4.dp))
							.padding(horizontal = 4.dp).padding(bottom = 2.dp)
							.offset(y = (-2).dp)
							.align(Alignment.TopEnd)
					)

					IconButton(
						onClick = { onRemoveClick(video) },
						modifier = Modifier
							.padding(bottom = 32.dp, end = 8.dp)
							.background(Color.DarkGray.copy(alpha = 0.8f), RoundedCornerShape(32.dp))
							.align(Alignment.BottomEnd)
					) {
						Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.button_desc_delete), tint = Color.LightGray)
					}

					IconButton(
						onClick = { onDumpClick(video) },
						modifier = Modifier
							.padding(bottom = 32.dp, start = 8.dp)
							.background(Color.DarkGray.copy(alpha = 0.8f), RoundedCornerShape(32.dp))
							.align(Alignment.BottomStart)
					) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.button_desc_delete), tint = Color.LightGray)
					}
				}
			}
			Box(Modifier.fillMaxSize().weight(0.5f).padding(8.dp)) {
				Column(Modifier.fillMaxSize().align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
					TextButton({
						onCancelClick()
					}, colors = buttonColors(backgroundColor = Color.Black)) {
						Text("Not duplicate", color = Color.White)
					}
				}
			}
		}
	}
}

fun formatTimestamp(totalSeconds: Int): String {
	val h = totalSeconds / 3600
	val m = (totalSeconds % 3600) / 60
	val s = totalSeconds % 60

	val mm = m.toString().padStart(2, '0')
	val ss = s.toString().padStart(2, '0')

	return if (h > 0) {
		"${h.toString().padStart(2, '0')}:$mm:$ss"
	} else {
		"$mm:$ss"
	}
}
