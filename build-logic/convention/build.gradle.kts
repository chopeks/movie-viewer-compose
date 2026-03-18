plugins {
    `kotlin-dsl`
}

group = "pl.chopeks.buildlogic"

dependencies {
    compileOnly(libs.bl.multiplatform.plugin)
    compileOnly(libs.bl.serialization.plugin)
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
    }
}
