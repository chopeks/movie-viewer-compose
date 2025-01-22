package pl.chopeks.movies.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Size
import pl.chopeks.movies.model.Actor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@OptIn(ExperimentalEncodingApi::class)
@Composable
fun ActorCard(
  actor: Actor,
  onClick: (Actor) -> Unit,
  onEditClick: (Actor) -> Unit
) {
  Card(Modifier.fillMaxWidth().clickable { onClick(actor) }, backgroundColor = Color.Black, elevation = 0.dp) {
    Box(Modifier.fillMaxWidth().aspectRatio(0.7f)) {
      val context = LocalPlatformContext.current
      AsyncImage(
        model = actor.image?.let {
          ImageRequest.Builder(context)
            .data(it.let(Base64.Mime::decode))
            .size(Size.ORIGINAL)
            .build()
        },
        contentDescription = actor.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.7f)
      )
      Row(
        modifier = Modifier.fillMaxWidth()
          .align(Alignment.BottomCenter)
          .background(Color.Black.copy(alpha = 0.6f)),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton({ onEditClick(actor) }, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Filled.Edit, "edit", tint = Color.White)
        }
        Spacer(Modifier.width(16.dp))
        Text(actor.name, color = Color.White)
      }
    }
  }
}
