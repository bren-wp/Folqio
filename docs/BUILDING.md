# Folqio — Android build instructions

Folqio is Android-only. Prerequisites: Android SDK platform 36, JDK 17, Gradle 8.13, Android Gradle Plugin 8.13.2.

From repository root:

    python3 scripts/audit_source.py
    gradle :android:testDebugUnitTest :android:lintDebug :android:assembleDebug :android:bundleRelease

Debug APK: android/build/outputs/apk/debug/android-debug.apk.
Unsigned release bundle without signing environment: android/build/outputs/bundle/release/android-release.aab.

**Google Play requires a securely signed release AAB**, not the debug APK or the unsigned bundle. The optional upload-signing environment uses FOLQIO_KEYSTORE_PATH, FOLQIO_KEYSTORE_PASSWORD, FOLQIO_KEY_ALIAS and FOLQIO_KEY_PASSWORD. Protect these secrets outside Git and release artifacts. Verify the release signing fingerprint before uploading.

CI runs source/Play invariants, Android unit tests, lint, APK build, and minified/shrunk release AAB. Build checks do not substitute for testing on real devices, Play Console declarations, or store acceptance. See [Testing](TESTING.md) and [Store readiness](STORE_READINESS.md).
