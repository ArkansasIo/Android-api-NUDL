# Noodle IDE Android Build

## Toolchain

- Android SDK 35
- JDK 17
- Gradle 8.11.1
- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3

## Debug build

From the repository root:

```bash
cd android
gradle :app:assembleDebug --no-daemon
```

APK output:

`android/app/build/outputs/apk/debug/app-debug.apk`

## Release build

```bash
cd android
gradle :app:assembleRelease --no-daemon
```

The release build uses `app/proguard-rules.pro`.

## CI

`.github/workflows/build-android.yml` builds the debug APK on Android source changes, packages the Android source tree, uploads both as an Actions artifact, and publishes them to the configured GitHub release when the build runs from `main`.

## Application

- Application ID: `com.arkansasio.nudl`
- Version: `1.5.1-pre-alpha`
- Version code: `2`
- Launcher: `NoodleWebParityActivity`
- Minimum Android: API 26
- Target Android: API 35
