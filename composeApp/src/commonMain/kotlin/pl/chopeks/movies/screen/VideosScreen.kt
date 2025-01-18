package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.cards.VideoCard
import pl.chopeks.movies.internal.screenmodel.VideosScreenModel
import pl.chopeks.movies.model.Actor
import pl.chopeks.movies.model.Category

class VideosScreen(
  private val actor: Actor? = null,
  private val category: Category? = null
) : Screen {
  private var editedVideoChips by mutableStateOf(0)

  @Composable
  override fun Content() {
    val screenModel = rememberScreenModel<VideosScreenModel>()
    val actorsBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val editActorsBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val categoriesBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val editCategoriesBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

    ScreenSkeleton(title = "", textActions = {
      TextButton({
        screenModel.changePage(Int.MIN_VALUE)
      }) { Text("Start".uppercase(), color = Color.Green.copy(alpha = 0.5f)) }
      TextButton({
        screenModel.changePage(-10)
      }) { Text("-10".uppercase(), color = Color.Green.copy(alpha = 0.5f)) }
      TextButton({
        screenModel.changePage(-1)
      }) { Text("-1".uppercase(), color = Color.Green.copy(alpha = 0.5f)) }
      TextButton({
        screenModel.changePage(1)
      }) { Text("+1".uppercase(), color = Color.Green.copy(alpha = 0.5f)) }
      TextButton({
        screenModel.changePage(10)
      }) { Text("+10".uppercase(), color = Color.Green.copy(alpha = 0.5f)) }
      TextButton({
        scope.launch { actorsBottomSheetState.show() }
      }) { Text("Actors", color = Color.Gray) }
      TextButton({
        scope.launch { categoriesBottomSheetState.show() }
      }) { Text("Categories", color = Color.Gray) }
    }, actions = {
      SortDropdownMenu(screenModel)
      Text("Page ${screenModel.currentPage} of ${screenModel.count}", color = Color.Green.copy(alpha = 0.6f))
    }) { scope ->
      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val items = screenModel.videos.chunked(5)
        items.forEach { chunk ->
          Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            chunk.forEach { video ->
              Box(modifier = Modifier.weight(1f)) {
                VideoCard(video, onClick = {
                  screenModel.play(it)
                }, onActorChipClick = {
                  scope.launch {
                    editedVideoChips = screenModel.videos.indexOf(video)
                    editActorsBottomSheetState.show()
                  }
                }, onCategoryChipClick = {
                  scope.launch {
                    editedVideoChips = screenModel.videos.indexOf(video)
                    editCategoriesBottomSheetState.show()
                  }
                }, onThumbnailClick = {
                  screenModel.generateThumbnail(video)
                }, onRemoveClick = {

                })
              }
            }
            repeat(5 - chunk.size) {
              Spacer(Modifier.weight(1f))
            }
          }
        }
        repeat(3 - items.size) {
          Spacer(Modifier.weight(1f))
        }
      }

      ActorsBottomSheet(screenModel, scope, actorsBottomSheetState)
      CategoriesBottomSheet(screenModel, scope, categoriesBottomSheetState)
      EditActorsBottomSheet(screenModel, editActorsBottomSheetState)
      EditCategoriesBottomSheet(screenModel, editCategoriesBottomSheetState)
    }

    LaunchedEffect(screenModel) {
      actor?.also { screenModel.selectedActors.add(it) }
      category?.also { screenModel.selectedCategories.add(it) }
      screenModel.init()
    }
    LaunchedEffect(screenModel.currentPage, screenModel.filter) {
      screenModel.getVideos()
    }
  }

  @OptIn(ExperimentalMaterialApi::class)
  @Composable
  fun SortDropdownMenu(screenModel: VideosScreenModel) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Sort by Date", "Sort by Duration")

    Column {
      ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        Text(
          text = options[screenModel.filter], color = Color.Gray, modifier = Modifier.width(180.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          options.forEachIndexed { index, option ->
            DropdownMenuItem(onClick = {
              screenModel.filter = options.indexOf(option)
              expanded = false
            }) {
              Text(text = option)
            }
          }
        }
      }
    }
  }

  @Composable
  fun ActorsBottomSheet(screenModel: VideosScreenModel, scope: CoroutineScope, modalBottomSheetState: ModalBottomSheetState) {
    ModalBottomSheetLayout(
      sheetState = modalBottomSheetState,
      sheetContent = {
        LazyVerticalGrid(
          GridCells.Fixed(3)
        ) {
          item {
            val actor = Actor(0, "None")
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(screenModel.selectedActors.firstOrNull { it.id == actor.id } != null, {
                val currentActor = screenModel.selectedActors.firstOrNull { it.id == actor.id }
                if (currentActor == null)
                  screenModel.selectedActors.add(actor)
                else
                  screenModel.selectedActors.remove(currentActor)
                scope.launch { screenModel.getVideos() }
              })
              Text(actor.name)
            }
          }

          items(screenModel.actors.toList()) { actor ->
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(screenModel.selectedActors.firstOrNull { it.id == actor.id } != null, {
                val currentActor = screenModel.selectedActors.firstOrNull { it.id == actor.id }
                if (currentActor == null)
                  screenModel.selectedActors.add(actor)
                else
                  screenModel.selectedActors.remove(currentActor)
                scope.launch { screenModel.getVideos() }
              })
              Text(actor.name)
            }
          }
        }
      },
      content = {}
    )
  }

  @Composable
  fun CategoriesBottomSheet(screenModel: VideosScreenModel, scope: CoroutineScope, modalBottomSheetState: ModalBottomSheetState) {
    ModalBottomSheetLayout(
      sheetState = modalBottomSheetState,
      sheetContent = {
        LazyVerticalGrid(
          GridCells.Fixed(3)
        ) {
          item {
            val category = Category(0, "None")
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(screenModel.selectedCategories.firstOrNull { it.id == category.id } != null, {
                val currentCategory = screenModel.selectedCategories.firstOrNull { it.id == category.id }
                if (currentCategory == null)
                  screenModel.selectedCategories.add(category)
                else
                  screenModel.selectedCategories.remove(currentCategory)
                scope.launch { screenModel.getVideos() }
              })
              Text(category.name)
            }
          }
          items(screenModel.categories.toList()) { category ->
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(screenModel.selectedCategories.firstOrNull { it.id == category.id } != null, {
                val currentCategory = screenModel.selectedCategories.firstOrNull { it.id == category.id }
                if (currentCategory == null)
                  screenModel.selectedCategories.add(category)
                else
                  screenModel.selectedCategories.remove(currentCategory)
                scope.launch { screenModel.getVideos() }
              })
              Text(category.name)
            }
          }
        }
      },
      content = {}
    )
  }

  @Composable
  fun EditActorsBottomSheet(screenModel: VideosScreenModel, modalBottomSheetState: ModalBottomSheetState) {
    if (editedVideoChips < screenModel.videos.size) {
      ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetContent = {
          LazyVerticalGrid(
            GridCells.Fixed(3)
          ) {
            items(screenModel.actors) { actor ->
              val video = screenModel.videos[editedVideoChips]
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(video.chips?.actors?.firstOrNull { it.id == actor.id } != null, {
                  screenModel.toggle(video, actor)
                })
                Text(actor.name)
              }
            }
          }
        },
        content = {}
      )
    }
  }

  @Composable
  fun EditCategoriesBottomSheet(screenModel: VideosScreenModel, modalBottomSheetState: ModalBottomSheetState) {
    if (editedVideoChips < screenModel.videos.size) {
      ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetContent = {
          LazyVerticalGrid(
            GridCells.Fixed(3)
          ) {
            items(screenModel.categories) { category ->
              val video = screenModel.videos[editedVideoChips]
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(video.chips?.categories?.firstOrNull { it.id == category.id } != null, {
                  screenModel.toggle(video, category)
                })
                Text(category.name)
              }
            }
          }
        },
        content = {}
      )
    }
  }
}