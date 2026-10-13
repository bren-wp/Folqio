# Folqio — App Store and Google Play release gate (October 2026)

This project is a **development preview**, not proof of approval from Google or Apple.

## Android / Google Play

- Google Play requires new Android phone apps and updates to target **API 36** from August 31, 2026. Folqio's target and compile SDK are 36.
- Use a Play-compliant **Android App Bundle** (`.aab`) and enrollment in Google Play App Signing, with an upload key controlled outside the repository. The CI `bundleRelease` artifact is unsigned when no signing env vars are provided.
- Never publish a debug-signed APK as a final release. Preserve and back up your upload keystore securely (outside GitHub).
- For a **local signed release**, securely provide these environment variables to Gradle: `FOLQIO_KEYSTORE_PATH`, `FOLQIO_KEYSTORE_PASSWORD`, `FOLQIO_KEY_ALIAS`, `FOLQIO_KEY_PASSWORD`. Then run `gradle :android:bundleRelease` and verify the certificate/upload key fingerprint before submission.
- Do not store keystores, export certificates, passwords, API keys, or private signing material in Git commits, CI logs, release assets or user-visible chat.
- Verify foreground/background, edge-to-edge and predictive back, insets, document pickers, photo merge, revoked grants, large/malformed PDF handling on API 26 and 36 devices.
- Complete Play Console Data safety, app content, privacy policy, accessibility, age rating, store listing, developer identity and content declarations. Keep descriptions restricted to implemented features.
- Provide adaptive app icon and Play listing artwork, including 512-pixel store icon, along with accurate screenshots. Visual 1:1 reference acceptance is still pending.

## Apple / App Store

- As of April 28, 2026, App Store Connect uploads must be built with **Xcode 26+ and the iOS 26 SDK+**. The CI uses GitHub macOS 26.
- The SwiftUI/PDFKit target supports iOS 16+, but deployment and build SDK are separate constraints.
- Apple Development/Distribution signing requires the real Apple Developer team, provisioning profiles, app identifier and entitlement validation. `DEVELOPMENT_TEAM` is intentionally not committed.
- Generate a valid `Assets.xcassets/AppIcon.appiconset` (1024×1024 App Store icon), iPhone/iPad launch and actual feature screenshots. **Not yet supplied**; this blocks readiness.
- Check every used required-reason API, bundled SDK and `PrivacyInfo.xcprivacy` declaration against the actual archive. Apple can reject invalid or incomplete manifests.
- Provide age rating, updated app privacy answers, EU DSA trader status where relevant, store listing, support/privacy policy URLs, accessibility and TestFlight QA.
- Test camera scanning and Files/Photos permissions on actual iPhones/iPads, including rotation, VoiceOver and Dynamic Type. Native CI currently compiles tests but may skip execution if no booted simulator is present.

## Distribution sign-off

- [ ] Android release bundle is signed with the correct upload key and verified
- [ ] iOS archive is signed with Apple Distribution identity; TestFlight validation passed
- [ ] Current native CI and device-farm integration tests are green
- [ ] No malformed PDFs or images cause crashes in exercised fuzz tests
- [ ] Every feature advertised on the listing is exercised end-to-end
- [ ] Real-device visual screenshots meet Folqio reference UI acceptance
- [ ] Icons, privacy policy, metadata, Data safety, privacy manifest and ratings reviewed
- [ ] No secrets in repo, workflows, logs or public releases

Sources: https://support.google.com/googleplay/android-developer/answer/11926878 and https://developer.apple.com/news/upcoming-requirements/
