package pl.chopeks.core.database

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import pl.chopeks.core.database.datasource.ActorLocalDataSource
import pl.chopeks.core.database.datasource.CategoriesDataSource
import pl.chopeks.core.database.datasource.DirectoriesLocalDataSource
import pl.chopeks.core.database.datasource.SettingsLocalDataSource
import pl.chopeks.core.database.datasource.VideoLocalDataSource

val databaseModule = DI.Module("databaseModule") {
	bindSingleton { DatabaseHelper.connect() }
	bindProvider { ActorLocalDataSource(instance()) }
	bindProvider { SettingsLocalDataSource() }
	bindProvider { DirectoriesLocalDataSource(instance()) }
	bindProvider { CategoriesDataSource(instance()) }
	bindProvider { VideoLocalDataSource(instance()) }
}