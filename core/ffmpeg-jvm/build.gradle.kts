plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.kotlinSerialization)
}

kotlin {
	jvm("desktop")

	sourceSets {
		val desktopMain by getting

		desktopMain.dependencies {
			api(projects.core.core)
			api(libs.kodein.di)
		}
	}
}
