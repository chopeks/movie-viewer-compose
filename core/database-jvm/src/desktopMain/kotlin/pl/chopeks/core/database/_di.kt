package pl.chopeks.core.database

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import pl.chopeks.core.database.datasource.*
import pl.chopeks.core.database.duplicates.AudioDedupLocalDataSource
import pl.chopeks.core.database.duplicates.FingerprintLocalDataSource

val databaseModule = DI.Module("databaseModule") {
	bindSingleton { DatabaseHelper.connect() }
	// for app
	bindProvider { ActorLocalDataSource(instance()) }
	bindProvider { SettingsLocalDataSource() }
	bindProvider { DirectoriesLocalDataSource(instance()) }
	bindProvider { CategoriesDataSource(instance()) }
	bindProvider { VideoLocalDataSource(instance()) }
	bindProvider { DuplicateLocalDataSource(instance()) }
	// for duplicate detection
	bindProvider { AudioDedupLocalDataSource(instance()) }
	bindProvider { FingerprintLocalDataSource(instance()) }
}