package pl.chopeks.movies.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DrawerState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pl.chopeks.movies.screen.*

@Composable
fun DrawerMenu(scope: CoroutineScope, drawerState: DrawerState) {
  val navigator = LocalNavigator.currentOrThrow

  Column(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
    Text(
      text = "Actors",
      modifier = Modifier
        .fillMaxWidth()
        .clickable {
          scope.launch {
            drawerState.close()
            navigator.replace(ActorsScreen())
          }
        }
        .padding(16.dp),
      color = Color.White
    )
    Text(
      text = "Categories",
      modifier = Modifier
        .fillMaxWidth()
        .clickable {
          scope.launch {
            drawerState.close()
            navigator.replace(CategoriesScreen())
          }
        }
        .padding(16.dp),
      color = Color.White
    )
    Text(
      text = "Videos",
      modifier = Modifier
        .fillMaxWidth()
        .clickable {
          scope.launch {
            drawerState.close()
            navigator.replace(VideosScreen())
          }
        }
        .padding(16.dp),
      color = Color.White
    )
    Text(
      text = "Duplicates",
      modifier = Modifier
        .fillMaxWidth()
        .clickable {
          scope.launch {
            drawerState.close()
            navigator.replace(DuplicatesScreen())
          }
        }
        .padding(16.dp),
      color = Color.White
    )
    Text(
      text = "Settings",
      modifier = Modifier
        .fillMaxWidth()
        .clickable {
          scope.launch {
            drawerState.close()
            navigator.replace(SettingsScreen())
          }
        }
        .padding(16.dp),
      color = Color.White
    )
  }
}