plugins {
	alias(libs.plugins.mv.desktop.library)
}

kotlin {
	sourceSets {
		desktopMain.dependencies {
			api(projects.core.core)
			api(projects.core.ffmpegJvm)
			api(libs.kodein.di)
		}
	}
}
