# Android API NUDL — Noodle IDE

Android API source package for **Noodle IDE 1.5.0-pre-alpha**.

Developer: Stephen Jr
Build ID: `NUDL-NOODLE-1.5.0-PRE-ALPHA-TEST1`

This repository is the Android-focused source destination for the NUDL Android API integration. The canonical source is developed in `ArkansasIo/NUDL-IDE` and mirrored here for Android APK development and testing.

## Target

- Android 10 / EMUI 10 and newer
- P20 Pro first target
- ARM64-compatible application path
- Debug APK produced by GitHub Actions
- App-level Noodle bootloader/startup screen

## Build

Open `android-api` as the Gradle Android project, or use the included GitHub Actions workflow when present.

The APK is an Android application artifact; the repository does not replace the phone's OEM hardware bootloader.
