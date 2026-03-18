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
import pl.chopeks.movies.composables.buttons.DrawerButton
import pl.chopeks.movies.screen.*

@Composable
fun DrawerMenu(scope: CoroutineScope, drawerState: DrawerState) {
  val navigator = LocalNavigator.currentOrThrow

  Column(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
    DrawerButton("A".repeat(1000), "[L.Alt+1]") {
      scope.launch {
        drawerState.close()
        navigator.replace(ActorsScreen())
      }
    }
    DrawerButton("Categories", "[L.Alt+2]") {
      scope.launch {
        drawerState.close()
        navigator.replace(CategoriesScreen())
      }
    }
    DrawerButton("Videos", "[L.Alt+3]") {
      scope.launch {
        drawerState.close()
        navigator.replace(VideosScreen())
      }
    }
    DrawerButton("Duplicates", "[L.Alt+4]") {
      scope.launch {
        drawerState.close()
        navigator.replace(DuplicatesScreen())
      }
    }
    DrawerButton("Settings") {
      scope.launch {
        drawerState.close()
        navigator.replace(SettingsScreen())
      }
    }
    DrawerButton("Logs") {
      scope.launch {
        drawerState.close()
        navigator.replace(LogScreen())
      }
    }
  }
}