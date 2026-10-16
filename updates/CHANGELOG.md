# Noodle IDE Android Patch History

## 1.5.0-pre-alpha TEST1

Build ID: `NUDL-NOODLE-1.5.0-PRE-ALPHA-TEST1`

### Included

- Android Noodle IDE bootloader/startup screen.
- NUDL editor and source editing surface.
- Analyze, Run, and TypeScript generation actions.
- Android NUDL API/service layer.
- Workspace/file persistence components.
- AndroidX-compatible Gradle configuration.
- GitHub Actions debug APK build.
- GitHub Release APK publishing.
- Machine-readable update manifest under `updates/latest.json`.
- Stable APK download endpoint through the GitHub Release asset.

### Verification

The validated build run completed successfully through APK compilation, source packaging, artifact upload, and GitHub Release publishing.

The APK SHA-256 is recorded in `updates/latest.json` so an updater can verify the downloaded package before installation.

### Update model

1. Read `updates/latest.json` from `main`.
2. Compare the installed version/build with `version` and `build`.
3. If newer, download the APK from the manifest's `apk.url`.
4. Verify the SHA-256 digest.
5. Hand the verified APK to Android's package installer.

Future patches should add a new version/build and update this manifest while keeping previous GitHub Release assets immutable.
