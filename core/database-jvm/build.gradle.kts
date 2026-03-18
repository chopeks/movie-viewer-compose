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

			implementation("org.xerial:sqlite-jdbc:3.48.0.0")
			implementation("org.apache.jdbm:jdbm:3.0-alpha5")
		}
	}
}
