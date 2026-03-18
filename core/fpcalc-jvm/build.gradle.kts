plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.kotlinSerialization)
	alias(libs.plugins.kotest)
}

kotlin {
	jvm("desktop") {
		testRuns.all {
			executionTask {
				useJUnitPlatform()
			}
		}
	}

	sourceSets {
		val desktopMain by getting
		val desktopTest by getting

		desktopMain.dependencies {
			api(projects.core.core)
			api(projects.core.ffmpegJvm)
			api(libs.kodein.di)
		}

		desktopTest.dependencies {
			implementation(libs.bundles.kotest.desktop)
		}
	}
}
