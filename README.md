## This app requires following apps in PATH:

There's check in settings screen that makes sure they're configured properly:

- `ffmpeg` tested with 8.1-full (for thumbnails, duplicate search, etc)
- `ffprobe` tested with 8.1 (for obtaining duration)
- `fpcalc` tested with 1.6.0 (for audio comparison)

### This is a Kotlin Multiplatform project targeting Web, Desktop.

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

Web version is still a little bit wonky and not included in jvm build yet.

You can open the web application by running the `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task.


To build wasm package to bundle for desktop app: 

`./gradlew composeApp:wasmJsBrowserDistribution`

To build desktop jar (if you want wasm included, call previous command first):

`./gradlew composeApp:packageUberJarForCurrentOS`

### Tests

`./gradlew allTests` - unit tests from all modules

### Credits

This project utilizes VMAF technology and models provided by Netflix under the BSD-2-Clause-Patent license.