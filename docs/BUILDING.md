# Build Folqio from source

For maintainers and contributors. The public [README](../README.md) describes Folqio's features and brand.

## Android

Requirements: Android SDK 36, JDK 17, Gradle 8.13, Android Gradle Plugin 8.13.2.

```sh
gradle :android:testDebugUnitTest :android:lintDebug :android:assembleDebug :android:bundleRelease
```

Debug APK: `android/build/outputs/apk/debug/android-debug.apk`. **Unsigned** release bundle: `android/build/outputs/bundle/release/android-release.aab`.

The AAB is not Google Play-uploadable until properly signed with the upload certificate. Secure upload signing uses `FOLQIO_KEYSTORE_PATH`, `FOLQIO_KEYSTORE_PASSWORD`, `FOLQIO_KEY_ALIAS` and `FOLQIO_KEY_PASSWORD`, stored outside source control.

## iOS

Requires Xcode 26+ and [XcodeGen](https://github.com/yonaskolb/XcodeGen). Run from the repository root:

```sh
swift ios/generate_app_icon.swift
xcodegen generate --spec ios/project.yml --project ios
xcodebuild build-for-testing -project ios/Folqio.xcodeproj -scheme Folqio -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO
```

The icon is generated locally from Folqio's vector identity; no runtime image requests or CDN dependencies. An iOS IPA requires signing, entitlement verification, and an Apple Developer account. See [store readiness](STORE_READINESS.md).

## Quality checks

GitHub Actions run Android unit tests and lint, build debug APK and unsigned release AAB, and compile the iOS app and test bundle. Compilation alone is not a substitute for device integration tests, permission tests, store certification or pixel-level UI comparisons. See [tests](TESTING.md) and [source audit](CODE_AUDIT.md).
