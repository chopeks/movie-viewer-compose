package pl.chopeks.movies.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
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
import pl.chopeks.core.model.Video
import pl.chopeks.core.model.VideoChips
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
fun VideoCard(
  video: Video,
  onClick: (Video) -> Unit,
  onActorChipClick: () -> Unit,
  onCategoryChipClick: () -> Unit,
  onThumbnailClick: () -> Unit,
  onRemoveClick: () -> Unit,
  onDumpClick: () -> Unit,
) {
  Card(Modifier.fillMaxSize(), backgroundColor = Color.DarkGray.copy(alpha = 0.5f), elevation = 0.dp) {
    Column(Modifier.fillMaxWidth()) {
      Box(Modifier.fillMaxWidth().aspectRatio(1.77f)) {
        val context = LocalPlatformContext.current
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

        ExpandableMenuButton(onThumbnailClick, onRemoveClick, onDumpClick)
      }
      ActorChips(video.chips, onActorChipClick)
      CategoryChips(video.chips, onCategoryChipClick)
    }
  }
}

@Composable
fun BoxScope.ExpandableMenuButton(
  onThumbnailClick: () -> Unit,
  onRemoveClick: () -> Unit,
  onDumpClick: () -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }  // To track if menu is expanded

  Box(modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).padding(bottom = 24.dp)) {
    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(32.dp))) {
      Icon(Icons.Filled.Menu, "Click to Expand", tint = Color.LightGray)
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.background(Color.Black.copy(alpha = 0.7f))
    ) {
      DropdownMenuItem(onClick = {
        onDumpClick()
        expanded = false
      }) {
        Text("Move to .dump", color = Color.White)
      }
      DropdownMenuItem(onClick = {
        onThumbnailClick()
        expanded = false
      }) {
        Text("Generate Thumbnail", color = Color.White)
      }
      DropdownMenuItem(onClick = {
        onRemoveClick()
        expanded = false
      }) {
        Text("Delete video", color = Color.White)
      }
    }
  }
}

@Composable
fun VideoChip(text: String, color: Color) {
  Row(modifier = Modifier.background(color, RoundedCornerShape(100.dp)).padding(horizontal = 6.dp).padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(text, color = Color.LightGray, modifier = Modifier.padding(start = 1.dp).offset(y = (-2).dp))
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActorChips(chips: VideoChips?, onActorClick: () -> Unit) {
  if (chips != null) {
    FlowRow(
      modifier = Modifier.padding(2.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      val color = Color(0xff674156)
      IconButton(onClick = { onActorClick() }, Modifier.background(color, RoundedCornerShape(100.dp)).size(28.dp)) {
        Icon(Icons.Default.Edit, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
      }
      chips.actors.forEach {
        VideoChip(it.name, color)
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(chips: VideoChips?, onCategoryClick: () -> Unit) {
  if (chips != null) {
    FlowRow(
      modifier = Modifier.padding(2.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      val color = Color(0xff1a5b98)
      IconButton(onClick = { onCategoryClick() }, Modifier.background(color, RoundedCornerShape(100.dp)).size(28.dp)) {
        Icon(Icons.Default.Edit, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
      }
      chips.categories.forEach {
        VideoChip(it.name, color)
      }
    }
  }
}