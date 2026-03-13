import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.kotlinSerialization)
	alias(libs.plugins.kotlinxRpc)
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
			api(libs.kotlinx.rpc.core)
		}

		desktopMain.dependencies {
			api(projects.core.databaseJvm)
			api(libs.kotlinx.rpc.server)
			api(libs.kotlinx.rpc.ktor.server)
		}

		wasmJsMain.dependencies {
			api(libs.ktor.client.js)
			api(libs.kotlinx.rpc.client)
			api(libs.kotlinx.rpc.ktor.client)
			api(libs.kotlinx.rpc.serialization)
		}
	}
}
