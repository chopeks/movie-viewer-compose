import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.kotlinSerialization)
}

kotlin {
	jvm("desktop")

	@OptIn(ExperimentalWasmDsl::class)
	wasmJs {
		browser()
		binaries.library()
	}


	sourceSets {
		val desktopMain by getting
		val wasmJsMain by getting

		commonMain.dependencies {
			api(projects.core.core)
			api(libs.kodein.di)
		}

		desktopMain.dependencies {
			api(projects.core.databaseJvm)
		}

		wasmJsMain.dependencies {
			api(libs.ktor.client.js)
		}
	}
}
