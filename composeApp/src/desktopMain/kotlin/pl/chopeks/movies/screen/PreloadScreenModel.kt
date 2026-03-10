package pl.chopeks.movies.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import pl.chopeks.core.database.DatabaseHelper
import pl.chopeks.movies.server.utils.Python
import pl.chopeks.movies.server.utils.RefreshUtils
import pl.chopeks.movies.tasks.TaskManager

class PreloadScreenModel(
	private val database: Lazy<Database>,
	private val taskManager: Lazy<TaskManager>
) : ScreenModel {

	var isDone by mutableStateOf(false)
	val events = mutableStateListOf<String>()

	fun init() {
		screenModelScope.launch {
			withContext(Dispatchers.IO) {
				val db = database.value
				events.add("Database connected, schema updated.")
				Python.init()

				DatabaseHelper.clean(db)
				events.add("Removed files purged.")
			}
			taskManager.value.start(events::add)
			isDone = true
		}
	}

}