plugins {
	alias(libs.plugins.mv.desktop.library)
}
kotlin {
	sourceSets {
		desktopMain.dependencies {
			api(projects.core.core)
			api(libs.kodein.di)
		}
	}
}
