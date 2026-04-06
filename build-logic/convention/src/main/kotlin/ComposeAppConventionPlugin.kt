import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import utils.enableContextParameters
import utils.libs

/**
 * Convention plugin that adds everything necessary to project to compile as desktop + wasm application.
 * Unit tests included and configured automatically
 */
abstract class ComposeAppConventionPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			pluginManager.apply(libs.findPlugin("kotlinMultiplatform").get().get().pluginId)
			pluginManager.apply(libs.findPlugin("composeMultiplatform").get().get().pluginId)
			pluginManager.apply(libs.findPlugin("composeCompiler").get().get().pluginId)
			pluginManager.apply(libs.findPlugin("kotlinSerialization").get().get().pluginId)
			pluginManager.apply(libs.findPlugin("kotlinxRpc").get().get().pluginId)
			pluginManager.apply(libs.findPlugin("kotest").get().get().pluginId)
			pluginManager.apply(libs.findPlugin("ksp").get().get().pluginId)

			extensions.configure(KotlinMultiplatformExtension::class.java) {
				jvm("desktop") {
					testRuns.all {
						executionTask {
							useJUnitPlatform()
						}
					}
				}

				@OptIn(ExperimentalWasmDsl::class)
				wasmJs {
					outputModuleName.value("composeApp")
					browser {
						val projectDirPath = project.projectDir.path
						commonWebpackConfig {
							outputFileName = "composeApp.js"
							devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
								static = (static ?: mutableListOf()).apply {
									add(projectDirPath)
								}
								port = 8081
							}
						}
					}
					binaries.executable()
				}

				sourceSets.apply {
					commonTest.dependencies {
						implementation(libs.findBundle("kotest").get())
					}
					desktopTest.dependencies {
						implementation(libs.findBundle("kotest.desktop").get())
					}
				}
			}
			enableContextParameters()
		}
	}
}
