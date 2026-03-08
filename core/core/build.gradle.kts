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
		commonMain.dependencies {
			api(libs.kotlinx.serialization.json)
		}
	}
}
