import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import utils.enableContextParameters
import utils.libs

val NamedDomainObjectContainer<KotlinSourceSet>.desktopMain: KotlinSourceSet
	get() = getByName("desktopMain")

val NamedDomainObjectContainer<KotlinSourceSet>.desktopTest: KotlinSourceSet
	get() = getByName("desktopTest")

/**
 * Convention plugin that adds everything necessary to project to compile as desktop/jvm library.
 * Unit tests included and configured automatically
 */
abstract class DesktopLibraryConventionPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		with(target) {
			pluginManager.apply(libs.findPlugin("kotlinMultiplatform").get().get().pluginId)
			pluginManager.apply(libs.findPlugin("kotlinSerialization").get().get().pluginId)
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

				sourceSets.apply {
					desktopTest.dependencies {
						implementation(libs.findBundle("kotest.desktop").get())
					}
				}
			}
			enableContextParameters()
		}
	}
}
