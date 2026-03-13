import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxRpc)
}

val versionString = "1.0.0"

kotlin {
    jvm("desktop")
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                    port = 8081
                }
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        val desktopMain by getting
        val wasmJsMain by getting

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

            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.coil.okhttp)

            implementation(libs.exposed.core)
            implementation(libs.exposed.dao)
            implementation(libs.exposed.jdbc)

            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.netty)
            implementation(libs.ktor.server.cors)
            implementation(libs.ktor.server.contentnegotiation)
            implementation(libs.ktor.server.gson)

            implementation(libs.kotlinx.rpc.server)
            implementation(libs.kotlinx.rpc.ktor.server)
            implementation(libs.kotlinx.rpc.serialization)

            implementation("org.imgscalr:imgscalr-lib:4.2")
            implementation("org.xerial:sqlite-jdbc:3.48.0.0")
            implementation("org.apache.jdbm:jdbm:3.0-alpha5")
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
