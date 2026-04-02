plugins {
	alias(libs.plugins.mv.desktop.library)
	alias(libs.plugins.mv.wasm.library)
}

kotlin {
	sourceSets {
		commonMain.dependencies {
			api(libs.kotlinx.serialization.json)
		}
	}
}
