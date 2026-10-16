# Noodle IDE Android Source

Noodle IDE is the Android client for the NUDL/Noodle development environment.

## Current build

- Version: `1.5.2-pre-alpha`
- Build ID: `NUDL-NOODLE-1.5.2-PRE-ALPHA-TEST3`
- Application ID: `com.arkansasio.nudl`
- Minimum Android: API 26 / Android 8.0
- Target/compile SDK: 35
- Java/Kotlin: 17
- Gradle CI: 8.11.1
- Primary launcher: `NoodleWebParityActivity`
- Developer: `Stephen Jr`

## Architecture

The Android application keeps the Noodle web-IDE layout while using native Android/Jetpack Compose components:

- startup/boot screen
- File/Edit/Build/View/Tools/Help menus
- solution/workspace explorer
- NUDL text editor
- interactive Blueprint graph surface
- node dragging and pin-to-pin connections
- inspector panel
- output/problems/console/AST/tokens dock
- C++/TypeScript/Python target selection
- NUDL API client, repository and ViewModel
- persistent Android workspace storage
- JSON workspace import/export
- Android launcher icon and adaptive icon resources

The Android UI is a native implementation of the web IDE structure; it does not embed the web page in a WebView.

## Build

CI uses a configured Gradle 8.11.1 installation on GitHub Actions. A Gradle wrapper JAR is intentionally not committed to this repository.

From an environment with Gradle 8.11.1 installed:

```bash
cd android
gradle :app:assembleDebug
```

The debug APK is produced at:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Installation note

CI debug APKs are debug-signed. Android only permits an in-place update when the new APK has the same signing certificate as the installed application. If an older APK was produced by another CI runner, uninstall that older debug build before installing the new one. Production/release signing should use a persistent keystore stored in CI secrets rather than committing a private key to Git.
