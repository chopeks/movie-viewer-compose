## This app requires following apps in PATH:
- `ffmpeg` (for thumbnails, duplicate search, etc)
- `ffprobe` (for obtaining duration)
- `fpcalc` (for audio comparison)

### This is a Kotlin Multiplatform project targeting Web, Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

You can open the web application by running the `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task.

### Tests

`./gradlew allTests` - unit tests from all modules

### Credits

This project utilizes VMAF technology and models provided by Netflix under the BSD-2-Clause-Patent license.