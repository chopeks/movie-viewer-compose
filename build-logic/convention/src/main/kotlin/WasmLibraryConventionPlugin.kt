import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import utils.libs

val NamedDomainObjectContainer<KotlinSourceSet>.wasmJsMain: KotlinSourceSet
	get() = getByName("wasmJsMain")

val NamedDomainObjectContainer<KotlinSourceSet>.wasmJsTest: KotlinSourceSet
	get() = getByName("wasmJsTest")

/**
 * Convention plugin that adds everything necessary to project to compile as wasm library.
 * Unit tests included and configured automatically
 */
abstract class WasmLibraryConventionPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			pluginManager.apply(libs.findPlugin("kotlinMultiplatform").get().get().pluginId)
			pluginManager.apply(libs.findPlugin("kotlinSerialization").get().get().pluginId)
			pluginManager.apply(libs.findPlugin("kotest").get().get().pluginId)
			pluginManager.apply(libs.findPlugin("ksp").get().get().pluginId)

			extensions.configure(KotlinMultiplatformExtension::class.java) {
				@OptIn(ExperimentalWasmDsl::class)
				wasmJs {
					browser()
					binaries.library()
				}

				sourceSets.apply {
					commonTest.dependencies {
						implementation(libs.findBundle("kotest").get())
					}
					wasmJsTest.dependencies {
						implementation(libs.findBundle("kotest").get())
					}
				}
			}
		}
	}
}
