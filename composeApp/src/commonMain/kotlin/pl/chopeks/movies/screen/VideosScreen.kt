package pl.chopeks.movies.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.launch
import movieviewer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import pl.chopeks.core.UiState
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Category
import pl.chopeks.movies.composables.ProgressIndicator
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.buttons.GreenTextButton
import pl.chopeks.movies.composables.cards.VideoCard
import pl.chopeks.movies.internal.screenmodel.VideosScreenModel
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation

class VideosScreen(
	private val actor: Actor? = null,
	private val category: Category? = null
) : Screen {
	enum class SheetType {
		NONE, ACTORS_FILTER, CATEGORIES_FILTER, EDIT_ACTORS, EDIT_CATEGORIES
	}

	@Composable
	override fun Content() {
		val screenModel = rememberScreenModel<VideosScreenModel>()
		val scope = rememberCoroutineScope()

		var currentSheet by remember { mutableStateOf(SheetType.NONE) }
		val actorsScrollState = rememberLazyGridState()
		val categoriesScrollState = rememberLazyGridState()
		val editActorsScrollState = rememberLazyGridState()
		val editCategoriesScrollState = rememberLazyGridState()
		val sheetState = rememberModalBottomSheetState(
			initialValue = ModalBottomSheetValue.Hidden,
			skipHalfExpanded = true,
			confirmValueChange = {
				if (it == ModalBottomSheetValue.Hidden)
					screenModel.setEditing(null)
				true
			})
		val openSheet: (SheetType) -> Unit = { type ->
			currentSheet = type
			scope.launch { sheetState.show() }
		}

		var removeConfirmDialog = remember { mutableStateOf(false) }

		val keyEventManager = localDI().direct.instance<KeyEventManager>()
		val navigator = LocalNavigator.current
		keyEventManager.setListener { onKeyEvent(it, navigator, screenModel) }

		val state by screenModel.uiState.collectAsState()
		val filterState by screenModel.filterState.collectAsState()

		ModalBottomSheetLayout(
			sheetState = sheetState,
			sheetGesturesEnabled = false,
			sheetContent = {
				Column {
					Row(Modifier.background(Color.Black).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
						IconButton(onClick = { scope.launch { sheetState.hide() } }) {
							Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.button_close))
						}
					}
					when (currentSheet) {
						SheetType.ACTORS_FILTER -> ActorsSheet(screenModel, actorsScrollState)
						SheetType.CATEGORIES_FILTER -> CategoriesSheet(screenModel, categoriesScrollState)
						SheetType.EDIT_ACTORS -> EditActorsSheet(screenModel, editActorsScrollState)
						SheetType.EDIT_CATEGORIES -> EditCategoriesSheet(screenModel, editCategoriesScrollState)
						SheetType.NONE -> Box(Modifier.size(0.dp))
					}
				}
			}
		) {
			ScreenSkeleton(title = "", leftActions = {
				GreenTextButton(stringResource(Res.string.button_start).uppercase()) { screenModel.changePage(Int.MIN_VALUE) }
				GreenTextButton("-10".uppercase()) { screenModel.changePage(-10) }
				GreenTextButton("-1".uppercase()) { screenModel.changePage(-1) }
				GreenTextButton("+1".uppercase()) { screenModel.changePage(1) }
				GreenTextButton("+10".uppercase()) { screenModel.changePage(10) }

				TextButton({
					scope.launch { openSheet(SheetType.ACTORS_FILTER) }
				}) { Text(stringResource(Res.string.button_actors), color = Color.Gray) }
				TextButton({
					scope.launch { openSheet(SheetType.CATEGORIES_FILTER) }
				}) { Text(stringResource(Res.string.button_categories), color = Color.Gray) }

				TextButton(onClick = screenModel::toggleSortFilter) {
					Text(listOf(stringResource(Res.string.label_sort_by_date), stringResource(Res.string.label_sort_by_duration))[filterState.filterType], color = Color.Gray)
				}
			}, rightActions = {
				when (val current = state) {
					is UiState.Success -> Text(stringResource(Res.string.label_video_pager, current.data.currentPage + 1, current.data.pageCount + 1), color = Color.Green.copy(alpha = 0.6f))
					else -> ProgressIndicator(modifier = Modifier)
				}
			}) { scope ->
				when (val current = state) {
					is UiState.Success -> Column(
						modifier = Modifier.fillMaxSize(),
						verticalArrangement = Arrangement.spacedBy(2.dp)
					) {
						for (rowIndex in 0 until 3) {
							Row(
								modifier = Modifier.weight(1f).fillMaxWidth(),
								horizontalArrangement = Arrangement.spacedBy(2.dp)
							) {
								for (columnIndex in 0 until 5) {
									val itemIndex = rowIndex * 5 + columnIndex

									val video = current.data.items.getOrNull(itemIndex)
									Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
										if (video != null) {
											VideoCard(
												video = video,
												onClick = { screenModel.play(it) },
												onActorChipClick = {
													scope.launch {
														screenModel.setEditing(video)
														openSheet(SheetType.EDIT_ACTORS)
													}
												},
												onCategoryChipClick = {
													scope.launch {
														screenModel.setEditing(video)
														openSheet(SheetType.EDIT_CATEGORIES)
													}
												},
												onThumbnailClick = {
													screenModel.generateThumbnail(video)
												},
												onRemoveClick = {
													screenModel.setEditing(video)
													removeConfirmDialog.value = true
												},
												onDumpClick = {
													screenModel.dump(video)
												}
											)
										} else {
											Box(Modifier.fillMaxSize())
										}
									}
								}
							}
						}
					}

					is UiState.Loading -> ProgressIndicator()
					is UiState.Error -> Text("Error: ${current.message}")
				}

				RemoveVideoDialog(removeConfirmDialog, screenModel)
			}

			LaunchedEffect(screenModel) {
				screenModel.init(actor, category)
			}
		}
	}

	@Composable
	fun ActorsSheet(screenModel: VideosScreenModel, scrollState: LazyGridState) {
		val filterParams by screenModel.filterState.collectAsState()
		val actors = listOf(Actor(0, stringResource(Res.string.checkbox_none))) + screenModel.actors

		LazyVerticalGrid(
			columns = GridCells.Fixed(3),
			state = scrollState
		) {
			items(actors) { actor ->
				val isSelected = filterParams.selectedActors.any { it.id == actor.id }
				Row(verticalAlignment = Alignment.CenterVertically) {
					Checkbox(isSelected, onCheckedChange = { screenModel.toggleSelection(actor) })
					Text(actor.name)
				}
			}
		}
	}

	@Composable
	fun CategoriesSheet(screenModel: VideosScreenModel, scrollState: LazyGridState) {
		val filterParams by screenModel.filterState.collectAsState()
		val categories = listOf(Category(0, stringResource(Res.string.checkbox_none))) + screenModel.categories

		LazyVerticalGrid(
			columns = GridCells.Fixed(3),
			state = scrollState
		) {
			items(categories) { category ->
				val isSelected = filterParams.selectedCategories.any { it.id == category.id }
				Row(verticalAlignment = Alignment.CenterVertically) {
					Checkbox(isSelected, onCheckedChange = { screenModel.toggleSelection(category) })
					Text(category.name)
				}
			}
		}
	}

	@Composable
	fun EditActorsSheet(screenModel: VideosScreenModel, scrollState: LazyGridState) {
		val video = screenModel.editingVideo ?: return
		LazyVerticalGrid(
			columns = GridCells.Fixed(3),
			state = scrollState
		) {
			items(screenModel.actors) { actor ->
				Row(verticalAlignment = Alignment.CenterVertically) {
					Checkbox(video.chips?.actors?.firstOrNull { it.id == actor.id } != null, {
						screenModel.toggleBinding(video, actor)
					})
					Text(actor.name)
				}
			}
		}
	}

	@Composable
	fun EditCategoriesSheet(screenModel: VideosScreenModel, scrollState: LazyGridState) {
		val video = screenModel.editingVideo ?: return
		LazyVerticalGrid(
			columns = GridCells.Fixed(3),
			state = scrollState
		) {
			items(screenModel.categories) { category ->
				Row(verticalAlignment = Alignment.CenterVertically) {
					Checkbox(video.chips?.categories?.firstOrNull { it.id == category.id } != null, {
						screenModel.toggleBinding(video, category)
					})
					Text(category.name)
				}
			}
		}
	}

	@Composable
	fun RemoveVideoDialog(show: MutableState<Boolean>, screenModel: VideosScreenModel) {
		if (show.value) {
			val video = screenModel.editingVideo ?: return
			AlertDialog(
				onDismissRequest = { show.value = false },
				title = { Text(stringResource(Res.string.confirmation_remove_title)) },
				text = { Text(stringResource(Res.string.confirmation_remove_desc, video.name)) },
				confirmButton = {
					Button(onClick = {
						show.value = false
						screenModel.remove(video)
					}, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)) {
						Text(stringResource(Res.string.button_remove), color = Color.White)
					}
				},
				dismissButton = {
					Button(onClick = { show.value = false }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
						Text(stringResource(Res.string.button_cancel), color = Color.LightGray)
					}
				}
			)
		}
	}

	private fun shortCutPlayVideo(index: Int, screenModel: VideosScreenModel): Boolean {
		screenModel.playVideoAtIndex(index)
		return true
	}

	private fun onKeyEvent(event: KeyEvent, navigator: Navigator?, screenModel: VideosScreenModel): Boolean {
		if (event.type != KeyEventType.KeyDown)
			return false

		if (KeyEventNavigation.onKeyEvent(event, navigator))
			return true

		if (event.isShiftPressed) {
			when (event.key) {
				Key.DirectionLeft, Key.Z -> {
					screenModel.changePage(-10)
					return true
				}

				Key.DirectionRight, Key.X -> {
					screenModel.changePage(10)
					return true
				}
			}
		}
		when (event.key) {
			Key(49) -> return shortCutPlayVideo(0, screenModel)
			Key(50) -> return shortCutPlayVideo(1, screenModel)
			Key(51) -> return shortCutPlayVideo(2, screenModel)
			Key(52) -> return shortCutPlayVideo(3, screenModel)
			Key(53) -> return shortCutPlayVideo(4, screenModel)
			Key.Q -> return shortCutPlayVideo(5, screenModel)
			Key.W -> return shortCutPlayVideo(6, screenModel)
			Key.E -> return shortCutPlayVideo(7, screenModel)
			Key.R -> return shortCutPlayVideo(8, screenModel)
			Key.T -> return shortCutPlayVideo(9, screenModel)
			Key.A -> return shortCutPlayVideo(10, screenModel)
			Key.S -> return shortCutPlayVideo(11, screenModel)
			Key.D -> return shortCutPlayVideo(12, screenModel)
			Key.F -> return shortCutPlayVideo(13, screenModel)
			Key.G -> return shortCutPlayVideo(14, screenModel)

			Key.DirectionLeft, Key.Z -> {
				screenModel.changePage(-1)
				return true
			}

			Key.DirectionRight, Key.X -> {
				screenModel.changePage(1)
				return true
			}
		}
		return false
	}
}