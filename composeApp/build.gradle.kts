import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
	alias(libs.plugins.mv.app.compose)
}

val versionString = "1.0.0"

kotlin {
	sourceSets {
		commonMain.dependencies {
			implementation(projects.core.core)
			implementation(projects.core.data)

			implementation("org.jetbrains.compose.runtime:runtime:1.10.2")
			implementation("org.jetbrains.compose.foundation:foundation:1.10.2")
			implementation("org.jetbrains.compose.material:material:1.10.2")
			implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
			implementation("org.jetbrains.compose.ui:ui:1.10.2")
			implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.10.2")
			implementation("org.jetbrains.compose.components:components-resources:1.10.2")

			implementation(libs.androidx.lifecycle.viewmodel)
			implementation(libs.androidx.lifecycle.runtime.compose)
			implementation(libs.voyager.navigator)
			implementation(libs.voyager.screenmodel)
			implementation(libs.voyager.kodein)

			implementation(libs.kodein.di.framework.compose)
			implementation(libs.napier)

			implementation(libs.ktor.client.core)
			implementation(libs.ktor.client.contentnegotiation)
			implementation(libs.ktor.client.json)
			implementation(libs.ktor.client.logging)
			implementation(libs.kotlinx.rpc.core)

			implementation(libs.coil.compose.core)
			implementation(libs.coil.compose)
			implementation(libs.coil.mp)
		}

		desktopMain.dependencies {
			implementation(projects.core.databaseJvm)
			implementation(projects.core.fpcalcJvm)
			implementation(projects.core.ffmpegJvm)

			implementation(compose.desktop.currentOs)
			implementation(libs.kotlinx.coroutines.swing)
			implementation(libs.ktor.client.okhttp)
			implementation(libs.coil.okhttp)

			implementation(libs.exposed.core)

			implementation(libs.ktor.server.core)
			implementation(libs.ktor.server.netty)
			implementation(libs.ktor.server.cors)
			implementation(libs.ktor.server.contentnegotiation)
			implementation(libs.ktor.server.gson)

			implementation(libs.kotlinx.rpc.server)
			implementation(libs.kotlinx.rpc.ktor.server)
			implementation(libs.kotlinx.rpc.serialization)

			implementation("org.imgscalr:imgscalr-lib:4.2")
		}

		wasmJsMain.dependencies {
			implementation(libs.ktor.client.js)
			implementation(libs.kotlinx.rpc.client)
			implementation(libs.kotlinx.rpc.ktor.client)
			implementation(libs.kotlinx.rpc.serialization)
		}

	}
}

compose.desktop {
	application {
		mainClass = "pl.chopeks.movies.MainKt"

		nativeDistributions {
			targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
			packageName = "pl.chopeks.movies"
			packageVersion = versionString
		}
	}
}
