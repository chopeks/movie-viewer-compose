package pl.chopeks.movies.screen

import org.jetbrains.exposed.sql.Database
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance
import pl.chopeks.core.data.ITaskManager

val platformScreenModule = DI.Module("platform_screen") {
	bindProvider { PreloadScreenModel(lazy { instance<Database>() }, lazy { instance<ITaskManager>() }) }
	bindProvider { SettingsPlatformScreenModel(instance(), instance()) }
}