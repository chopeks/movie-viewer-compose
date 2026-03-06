package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Size
import kotlinx.coroutines.launch
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.cards.ActorCard
import pl.chopeks.movies.internal.screenmodel.ActorsScreenModel
import pl.chopeks.movies.model.Actor
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation

class ActorsScreen : Screen {
  @Composable
  override fun Content() {
    val addDialog = remember { mutableStateOf(false) }
    val editDialog = remember { mutableStateOf<Actor?>(null) }
    val screenModel = rememberScreenModel<ActorsScreenModel>()
    val keyEventManager = localDI().direct.instance<KeyEventManager>()
    val navigator = LocalNavigator.current
    keyEventManager.setListener { KeyEventNavigation.onKeyEvent(it, navigator) }

    ScreenSkeleton(
      title = "Actors",
      textActions = {
        TextButton({ addDialog.value = true }) { Text("Add Actors".uppercase(), color = Color.Green.copy(alpha = 0.5f)) }
      },
      actions = {
        TextField(
          screenModel.searchFilter, { screenModel.searchFilter = it },
          trailingIcon = {
            if (screenModel.searchFilter.isEmpty())
              Icon(Icons.Default.Search, contentDescription = "Search")
            else
              IconButton(onClick = { screenModel.searchFilter = "" }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
              }
          },
          label = { Text("Filter") },
          colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Black)
        )
      }
    ) { scope ->
      Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val items = screenModel.filteredActors.chunked(8)
        items.forEach { chunk ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Spacer(Modifier.fillMaxWidth(0.08f))
            chunk.forEach { actor ->
              Box(modifier = Modifier.weight(1f)) {
                ActorCard(
                  actor,
                  onClick = {
                    scope.launch {
                      navigator?.replace(VideosScreen(actor = it))
                    }
                  },
                  onEditClick = {
                    editDialog.value = it
                  })
              }
            }
            repeat(8 - chunk.size) {
              Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.fillMaxWidth(0.08f))
          }
        }
      }
    }

    AddActorDialog(addDialog, screenModel)
    EditActorDialog(editDialog, screenModel)

    LaunchedEffect(Unit) {
      screenModel.getActors()
    }
    LaunchedEffect(screenModel.searchFilter) {
      screenModel.filter()
    }
  }

  @Composable
  fun AddActorDialog(show: MutableState<Boolean>, screenModel: ActorsScreenModel) {
    if (show.value) {
      val context = LocalPlatformContext.current
      var name by remember { mutableStateOf("") }
      var url by remember { mutableStateOf("") }
      AlertDialog(
        onDismissRequest = { show.value = false },
        title = { Text("Add actor") },
        text = {
          Column(Modifier.fillMaxWidth()) {
            TextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            TextField(url, { url = it }, label = { Text("Image url") }, modifier = Modifier.fillMaxWidth())
            AsyncImage(
              ImageRequest.Builder(context)
                .data(url)
                .size(Size.ORIGINAL)
                .build(), null, Modifier.fillMaxWidth().aspectRatio(1.77f)
            )
          }
        },
        confirmButton = {
          Button(onClick = {
            if (name.isNotBlank()) {
              screenModel.add(name, url)
              show.value = false
            }
          }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
            Text("Add", color = Color.White)
          }
        },
        dismissButton = {
          Button(onClick = { show.value = false }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
            Text("Cancel", color = Color.LightGray)
          }
        }
      )
    }
  }

  @Composable
  fun EditActorDialog(actor: MutableState<Actor?>, screenModel: ActorsScreenModel) {
    if (actor.value != null) {
      val context = LocalPlatformContext.current
      var name by remember { mutableStateOf(actor.value!!.name) }
      var url by remember { mutableStateOf(if ((actor.value!!.image ?: "").startsWith("http:")) actor.value!!.image ?: "" else "") }
      AlertDialog(
        onDismissRequest = { actor.value = null },
        title = { Text("Edit actor") },
        text = {
          Column(Modifier.fillMaxWidth()) {
            TextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            TextField(url, { url = it }, label = { Text("Image url") }, modifier = Modifier.fillMaxWidth())
            AsyncImage(
              ImageRequest.Builder(context)
                .data(url)
                .size(Size.ORIGINAL)
                .build(), null, Modifier.fillMaxWidth().aspectRatio(1.77f)
            )
          }
        },
        confirmButton = {
          Button(onClick = {
            if (name.isNotBlank()) {
              screenModel.edit(actor.value!!, name, url)
              actor.value = null
            }
          }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
            Text("Confirm", color = Color.White)
          }
        },
        dismissButton = {
          Button(onClick = { actor.value = null }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
            Text("Cancel", color = Color.LightGray)
          }
        }
      )
    }
  }
}