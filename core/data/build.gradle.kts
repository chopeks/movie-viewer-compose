plugins {
	alias(libs.plugins.mv.desktop.library)
	alias(libs.plugins.mv.wasm.library)
	alias(libs.plugins.kotlinxRpc)
}

kotlin {
	sourceSets {
		commonMain.dependencies {
			api(projects.core.core)
			api(libs.kodein.di)
			api(libs.kotlinx.rpc.core)
		}

		desktopMain.dependencies {
			api(projects.core.databaseJvm)
			api(libs.kotlinx.rpc.server)
			api(libs.kotlinx.rpc.ktor.server)
			api(projects.core.ffmpegJvm)
		}

		wasmJsMain.dependencies {
			api(libs.ktor.client.js)
			api(libs.kotlinx.rpc.client)
			api(libs.kotlinx.rpc.ktor.client)
			api(libs.kotlinx.rpc.serialization)
		}
	}
}
