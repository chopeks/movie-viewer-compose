package pl.chopeks.movies.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells.Fixed
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import org.kodein.di.compose.rememberInstance
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video
import pl.chopeks.movies.composables.ProgressIndicator
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.buttons.GreenTextButton
import pl.chopeks.movies.composables.cards.VideoCard
import pl.chopeks.movies.composables.state.rememberAlertDialogState
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation
import pl.chopeks.movies.utils.collectAsSuccessState
import pl.chopeks.screenmodel.VideosScreenModel
import pl.chopeks.screenmodel.model.UiState

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
		val keyEventManager by rememberInstance<KeyEventManager>()
		val navigator = LocalNavigator.current

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

		val removeConfirmDialogState = rememberAlertDialogState()

		DisposableEffect(keyEventManager, navigator, screenModel) {
			keyEventManager.setListener { onKeyEvent(it, navigator, screenModel) }
			onDispose { keyEventManager.setListener(null) }
		}

		LaunchedEffect(screenModel) {
			screenModel.init(actor, category)
		}

		val state by screenModel.uiState.collectAsState()

		ModalBottomSheetLayout(
			sheetState = sheetState,
			sheetGesturesEnabled = false,
			sheetContent = {
				val toolbarState by screenModel.toolbarState.collectAsState()
				when (val current = toolbarState) {
					is UiState.Error -> Text("Error: ${current.message}")
					UiState.Loading -> ProgressIndicator()
					is UiState.Success -> {
						Column {
							Row(
								modifier = Modifier.background(Color.Black).fillMaxWidth(),
								horizontalArrangement = Arrangement.End
							) {
								IconButton(onClick = { scope.launch { sheetState.hide() } }) {
									Icon(
										imageVector = Icons.Default.Close,
										contentDescription = stringResource(Res.string.button_close)
									)
								}
							}
							when (currentSheet) {
								SheetType.ACTORS_FILTER -> ActorsSheet(current.data, screenModel, actorsScrollState)
								SheetType.CATEGORIES_FILTER -> CategoriesSheet(current.data, screenModel, categoriesScrollState)
								SheetType.EDIT_ACTORS -> EditActorsSheet(current.data, screenModel, editActorsScrollState)
								SheetType.EDIT_CATEGORIES -> EditCategoriesSheet(current.data, screenModel, editCategoriesScrollState)
								SheetType.NONE -> Box(Modifier.size(0.dp))
							}
						}
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

				TextButton(onClick = { openSheet(SheetType.ACTORS_FILTER) }) {
					Text(stringResource(Res.string.button_actors), color = Color.Gray)
				}
				TextButton(onClick = { openSheet(SheetType.CATEGORIES_FILTER) }) {
					Text(stringResource(Res.string.button_categories), color = Color.Gray)
				}

				TextButton(onClick = screenModel::toggleSortFilter) {
					val filterType = screenModel.toolbarState.collectAsSuccessState(initial = 0) { it.filters.filterType }
					Text(
						text = listOf(
							stringResource(Res.string.label_sort_by_date),
							stringResource(Res.string.label_sort_by_duration)
						)[filterType],
						color = Color.Gray
					)
				}
			}, rightActions = {
				val page = screenModel.uiState.collectAsSuccessState(initial = null) { it }
				if (page != null) {
					Text(
						text = stringResource(Res.string.label_video_pager, page.currentPage + 1, page.pageCount + 1),
						color = Color.Green.copy(alpha = 0.6f)
					)
				}
			}) {
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
													screenModel.setEditing(video)
													openSheet(SheetType.EDIT_ACTORS)
												},
												onCategoryChipClick = {
													screenModel.setEditing(video)
													openSheet(SheetType.EDIT_CATEGORIES)
												},
												onThumbnailClick = {
													screenModel.generateThumbnail(video)
												},
												onRemoveClick = {
													screenModel.setEditing(video)
													removeConfirmDialogState.show()
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

				RemoveVideoDialog(
					isVisible = removeConfirmDialogState.isVisible,
					onDismiss = { removeConfirmDialogState.hide() },
					onConfirm = { video ->
						screenModel.remove(video)
						removeConfirmDialogState.hide()
					},
					editingVideo = screenModel.editingVideo.collectAsState().value
				)
			}
		}
	}

	@Composable
	fun ActorsSheet(
		state: VideosScreenModel.ToolbarState,
		screenModel: VideosScreenModel,
		scrollState: LazyGridState
	) {
		val noneText = stringResource(Res.string.checkbox_none)
		val actors = remember(state.actors, noneText) {
			listOf(Actor(0, noneText)) + state.actors
		}
		LazyVerticalGrid(
			columns = Fixed(3),
			state = scrollState
		) {
			items(actors) { actor ->
				val isSelected by remember(actor.id, state.filters.selectedActors) {
					derivedStateOf { state.filters.selectedActors.any { it.id == actor.id } }
				}
				Row(verticalAlignment = Alignment.CenterVertically) {
					Checkbox(isSelected, onCheckedChange = { screenModel.toggleSelection(actor) })
					Text(actor.name)
				}
			}
		}
	}

	@Composable
	fun CategoriesSheet(
		state: VideosScreenModel.ToolbarState,
		screenModel: VideosScreenModel,
		scrollState: LazyGridState
	) {
		val noneText = stringResource(Res.string.checkbox_none)
		val categories = remember(state.categories, noneText) {
			listOf(Category(0, noneText)) + state.categories
		}

		LazyVerticalGrid(
			columns = Fixed(3),
			state = scrollState
		) {
			items(categories) { category ->
				val isSelected by remember(category.id, state.filters.selectedCategories) {
					derivedStateOf { state.filters.selectedCategories.any { it.id == category.id } }
				}
				Row(verticalAlignment = Alignment.CenterVertically) {
					Checkbox(isSelected, onCheckedChange = { screenModel.toggleSelection(category) })
					Text(category.name)
				}
			}
		}
	}

	@Composable
	fun EditActorsSheet(
		state: VideosScreenModel.ToolbarState,
		screenModel: VideosScreenModel,
		scrollState: LazyGridState
	) {
		EditGridSheet(screenModel, scrollState, state.actors) { video, actor ->
			val isSelected by remember(actor.id, video.chips?.actors) {
				derivedStateOf { video.chips?.actors?.any { it.id == actor.id } == true }
			}
			Row(verticalAlignment = Alignment.CenterVertically) {
				Checkbox(isSelected, onCheckedChange = {
					screenModel.toggleBinding(video, actor)
				})
				Text(actor.name)
			}
		}
	}

	@Composable
	fun EditCategoriesSheet(
		state: VideosScreenModel.ToolbarState,
		screenModel: VideosScreenModel,
		scrollState: LazyGridState
	) {
		EditGridSheet(screenModel, scrollState, state.categories) { video, category ->
			val isSelected by remember(category.id, video.chips?.categories) {
				derivedStateOf { video.chips?.categories?.any { it.id == category.id } == true }
			}
			Row(verticalAlignment = Alignment.CenterVertically) {
				Checkbox(isSelected, onCheckedChange = {
					screenModel.toggleBinding(video, category)
				})
				Text(category.name)
			}
		}
	}

	@Composable
	private fun RemoveVideoDialog(
		isVisible: Boolean,
		onDismiss: () -> Unit,
		onConfirm: (Video) -> Unit,
		editingVideo: Video?
	) {
		if (isVisible && editingVideo != null) {
			AlertDialog(
				onDismissRequest = onDismiss,
				title = { Text(stringResource(Res.string.confirmation_remove_title)) },
				text = { Text(stringResource(Res.string.confirmation_remove_desc, editingVideo.name)) },
				confirmButton = {
					Button(
						onClick = { onConfirm(editingVideo) },
						colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
					) {
						Text(stringResource(Res.string.button_remove), color = Color.White)
					}
				},
				dismissButton = {
					Button(
						onClick = onDismiss,
						colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
					) {
						Text(stringResource(Res.string.button_cancel), color = Color.LightGray)
					}
				}
			)
		}
	}

	@Composable
	private fun <T> EditGridSheet(
		screenModel: VideosScreenModel,
		scrollState: LazyGridState,
		container: List<T>,
		item: @Composable (Video, T) -> Unit
	) {
		val video by screenModel.editingVideo.collectAsState()
		video?.let { currentVideo ->
			LazyVerticalGrid(
				columns = Fixed(3),
				state = scrollState
			) {
				items(container) {
					item(currentVideo, it)
				}
			}
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
			Key.One -> return shortCutPlayVideo(0, screenModel)
			Key.Two -> return shortCutPlayVideo(1, screenModel)
			Key.Three -> return shortCutPlayVideo(2, screenModel)
			Key.Four -> return shortCutPlayVideo(3, screenModel)
			Key.Five -> return shortCutPlayVideo(4, screenModel)
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
