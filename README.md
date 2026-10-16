# Android API NUDL — Noodle IDE

Android client and API integration for **Noodle IDE 1.5.2-pre-alpha**.

- Build ID: `NUDL-NOODLE-1.5.2-PRE-ALPHA-TEST3`
- Application ID: `com.arkansasio.nudl`
- Minimum Android: Android 8.0 / API 26
- Compile and target SDK: 35
- Language: Kotlin
- UI: Jetpack Compose / Material 3
- Networking: Retrofit + OkHttp + Gson
- CI build: GitHub Actions using JDK 17 and Gradle 8.11.1

The Android application is a native client for the NUDL/Noodle development environment. Its code is housed in `android/src/main/kotlin` and included in the Gradle app module via the module's `sourceSets` configuration.

## Project layout

```text
android/
  app/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/res/
    src/androidTest/
  src/main/kotlin/com/arkansasio/nudl/
    MainActivity.kt
    NoodleWebParityActivity.kt
    WebStyleActivity.kt
    NudlVmFactory.kt
    api/
      NudlApiClient.kt
      NudlApiConfig.kt
      NudlApiModels.kt
      NudlApiService.kt
      NudlRepository.kt
      NudlViewModel.kt
      NudlWorkspace*.kt
  build.gradle.kts
  settings.gradle.kts
.github/workflows/noodle-parallel-build.yml
updates/latest.json
```

## Build a debug APK

Requirements: JDK 17, Android SDK platform/build-tools 35, and Gradle 8.11.1.

```bash
cd android
gradle :app:assembleDebug --no-daemon
```

APK output:

`android/app/build/outputs/apk/debug/app-debug.apk`

Or open the repository in Android Studio and open the `android/` folder as the Gradle project.

## GitHub Actions

The `Noodle IDE Parallel Build` workflow runs on pushes to `main` and can be started manually from the Actions tab. It validates required source files, builds the debug APK, calculates a SHA-256 checksum, and uploads both APK and source-package artifacts.

The APK is **debug-signed** for testing. For consistent in-place updates, sign all releases with the same persistent signing key; store release keystore credentials in GitHub Actions secrets, never commit them to the repository.

## NUDL API

The Retrofit client defaults to `http://10.0.2.2:8787/`, the Android emulator's host loopback address for a development API running on the host machine. A physical device should use the host's reachable LAN address or a deployed HTTPS endpoint. Keep server URLs configurable and do not ship development endpoints in production builds.

The API service describes these endpoints:

- `GET /api/v1/health`
- `POST /api/v1/nudl/lex`
- `POST /api/v1/nudl/parse`
- `POST /api/v1/nudl/analyze`
- `POST /api/v1/nudl/run`
- `POST /api/v1/nudl/generate`

These routes must be implemented by the paired NUDL API backend. They are not supplied by the Android app itself.

## Update manifest

`updates/latest.json` describes the intended pre-alpha release. Actual artifact IDs and checksums are authoritative only after the corresponding workflow run completes successfully.

## Scope and limitations

This is an app-level development environment, not a replacement for a phone's OEM bootloader or Android system firmware. The NUDL server performs backend language operations; the Android client provides its UI and HTTP integration.
