import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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

			implementation(libs.exposed.core)
			implementation(libs.exposed.dao)
			implementation(libs.exposed.jdbc)

			implementation("org.xerial:sqlite-jdbc:3.48.0.0")
			implementation("org.apache.jdbm:jdbm:3.0-alpha5")
		}
	}
}
