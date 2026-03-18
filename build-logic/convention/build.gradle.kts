plugins {
    `kotlin-dsl`
}

group = "pl.chopeks.buildlogic"

dependencies {
    compileOnly(libs.bl.multiplatform.plugin)
    compileOnly(libs.bl.compose.plugin)
    compileOnly(libs.bl.compose.compiler.plugin)
    compileOnly(libs.bl.serialization.plugin)
    compileOnly(libs.bl.rpc.plugin)
    compileOnly(libs.bl.ksp.plugin)
    compileOnly(libs.bl.kotest.plugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("desktopLibrary") {
            id = libs.plugins.mv.desktop.library.get().pluginId
            implementationClass = "DesktopLibraryConventionPlugin"
        }
        register("wasmLibrary") {
            id = libs.plugins.mv.wasm.library.get().pluginId
            implementationClass = "WasmLibraryConventionPlugin"
        }
        register("composeApp") {
            id = libs.plugins.mv.app.compose.get().pluginId
            implementationClass = "ComposeAppConventionPlugin"
        }
    }
}
