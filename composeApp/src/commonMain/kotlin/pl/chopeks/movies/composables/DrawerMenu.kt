package pl.chopeks.movies.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import movieviewer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import pl.chopeks.movies.composables.buttons.DrawerButton
import pl.chopeks.movies.screen.*

@Composable
fun DrawerMenu(scope: CoroutineScope, drawerState: DrawerState) {
	val navigator = LocalNavigator.currentOrThrow

	Column(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
		DrawerButton(stringResource(Res.string.drawer_button_actors), stringResource(Res.string.drawer_shortcut_actors)) {
			scope.launch {
				drawerState.close()
				navigator.replace(ActorsScreen())
			}
		}
		DrawerButton(stringResource(Res.string.drawer_button_categories), stringResource(Res.string.drawer_shortcut_categories)) {
			scope.launch {
				drawerState.close()
				navigator.replace(CategoriesScreen())
			}
		}
		DrawerButton(stringResource(Res.string.drawer_button_videos), stringResource(Res.string.drawer_shortcut_videos)) {
			scope.launch {
				drawerState.close()
				navigator.replace(VideosScreen())
			}
		}
		DrawerButton(stringResource(Res.string.drawer_button_duplicates), stringResource(Res.string.drawer_shortcut_duplicates)) {
			scope.launch {
				drawerState.close()
				navigator.replace(DuplicatesScreen())
			}
		}
		DrawerButton(stringResource(Res.string.drawer_button_encoder)) {
			scope.launch {
				drawerState.close()
				navigator.replace(EncoderScreen())
			}
		}
		DrawerButton(stringResource(Res.string.drawer_button_settings)) {
			scope.launch {
				drawerState.close()
				navigator.replace(SettingsScreen())
			}
		}
		DrawerButton(stringResource(Res.string.drawer_button_logs)) {
			scope.launch {
				drawerState.close()
				navigator.replace(LogScreen())
			}
		}
	}
}