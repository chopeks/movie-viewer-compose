package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Size
import kotlinx.coroutines.launch
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.cards.CategoryCard
import pl.chopeks.movies.internal.screenmodel.CategoriesScreenModel
import pl.chopeks.movies.model.Category
import pl.chopeks.movies.utils.KeyEventManager

class CategoriesScreen : Screen {
  @Composable
  override fun Content() {
    val addDialog = remember { mutableStateOf(false) }
    val editDialog = remember { mutableStateOf<Category?>(null) }
    val screenModel = rememberScreenModel<CategoriesScreenModel>()
    val keyEventManager = localDI().direct.instance<KeyEventManager>()
    val navigator = LocalNavigator.current
    keyEventManager.setListener { onKeyEvent(it, navigator) }

    ScreenSkeleton(title = "Categories", textActions = {
      TextButton({
        addDialog.value = true
      }) { Text("Add category".uppercase(), color = Color.Green.copy(alpha = 0.5f)) }
    }) { scope ->
      Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val items = screenModel.categories.chunked(6)
        items.forEach { chunk ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            chunk.forEach { category ->
              Box(modifier = Modifier.weight(1f)) {
                CategoryCard(category, onClick = {
                  scope.launch {
                    navigator?.replace(VideosScreen(category = it))
                  }
                }, onEditClick = {
                  editDialog.value = it
                })
              }
            }
            repeat(6 - chunk.size) {
              Spacer(Modifier.weight(1f))
            }
          }
        }
      }
    }

    AddCategoryDialog(addDialog, screenModel)
    EditCategoryDialog(editDialog, screenModel)

    LaunchedEffect(Unit) {
      screenModel.getCategories()
    }
  }

  @Composable
  fun AddCategoryDialog(show: MutableState<Boolean>, screenModel: CategoriesScreenModel) {
    if (show.value) {
      val context = LocalPlatformContext.current
      var name by remember { mutableStateOf("") }
      var url by remember { mutableStateOf("") }
      AlertDialog(onDismissRequest = { show.value = false }, title = { Text("Add category") }, text = {
        Column(Modifier.fillMaxWidth()) {
          TextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
          TextField(url, { url = it }, label = { Text("Image url") }, modifier = Modifier.fillMaxWidth())
          AsyncImage(
            ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).build(), null, Modifier.fillMaxWidth().aspectRatio(1.77f)
          )
        }
      }, confirmButton = {
        Button(onClick = {
          if (name.isNotBlank()) {
            screenModel.add(name, url)
            show.value = false
          }
        }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
          Text("Add", color = Color.White)
        }
      }, dismissButton = {
        Button(onClick = { show.value = false }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
          Text("Cancel", color = Color.LightGray)
        }
      })
    }
  }

  @Composable
  fun EditCategoryDialog(actor: MutableState<Category?>, screenModel: CategoriesScreenModel) {
    if (actor.value != null) {
      val context = LocalPlatformContext.current
      var name by remember { mutableStateOf(actor.value!!.name) }
      var url by remember { mutableStateOf(actor.value!!.image ?: "") }
      AlertDialog(onDismissRequest = { actor.value = null }, title = { Text("Edit category") }, text = {
        Column(Modifier.fillMaxWidth()) {
          TextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
          TextField(url, { url = it }, label = { Text("Image url") }, modifier = Modifier.fillMaxWidth())
          AsyncImage(
            ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).build(), null, Modifier.fillMaxWidth().aspectRatio(1.77f)
          )
        }
      }, confirmButton = {
        Button(onClick = {
          if (name.isNotBlank()) {
            screenModel.edit(actor.value!!, name, url)
            actor.value = null
          }
        }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
          Text("Confirm", color = Color.White)
        }
      }, dismissButton = {
        Button(onClick = { actor.value = null }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
          Text("Cancel", color = Color.LightGray)
        }
      })
    }
  }

  private fun onKeyEvent(event: KeyEvent, navigator: Navigator?): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (event.isAltPressed) {
      when (event.key) {
        Key(49) -> {
          navigator?.replace(ActorsScreen()); return true
        }

        Key(50) -> {
          navigator?.replace(CategoriesScreen()); return true
        }

        Key(51) -> {
          navigator?.replace(VideosScreen()); return true
        }
      }
    }
    return false
  }
}