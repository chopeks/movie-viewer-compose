plugins {
	alias(libs.plugins.mv.desktop.library)
}

kotlin {
	sourceSets {
		desktopMain.dependencies {
			api(projects.core.core)
			api(libs.kodein.di)

			implementation(libs.exposed.core)
			implementation(libs.exposed.dao)
			implementation(libs.exposed.jdbc)

			implementation(libs.sqlite.jdbc)
			implementation(libs.sqlite.jdbm)

			implementation(libs.androidx.datastore.core)
			implementation(libs.androidx.datastore.core.okio)
		}
	}
}
