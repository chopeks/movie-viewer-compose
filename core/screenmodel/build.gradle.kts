plugins {
	alias(libs.plugins.mv.desktop.library)
	alias(libs.plugins.mv.wasm.library)
}

kotlin {
	sourceSets {
		commonMain.dependencies {
			api(projects.core.core)
			api(projects.core.data)
			api(libs.kodein.di)
			api(libs.voyager.screenmodel)
		}
	}
}
