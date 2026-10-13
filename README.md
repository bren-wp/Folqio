# Folqio

**Your documents. Your device.**

Folqio is an offline-first PDF reading and document-management app developed independently for **Android (Kotlin + Jetpack Compose)** and **iOS (Swift + SwiftUI + PDFKit)**. Its visual system follows the dark navy, slate and mint Folqio brand.

> **Status: initial development foundation — not a production release.** This repository previously contained only a README and an MIT license. The code introduced here is the first functional baseline, not a claim that all concept screens or advanced editing features are complete.

## Implemented

| Feature | Android | iOS |
| --- | --- | --- |
| Local PDF import | System document picker | System Files picker + local copy |
| Open/read PDF | Android PdfRenderer | Apple PDFKit |
| Page navigation | Previous/next + bitmap pinch zoom | PDFKit swipe/zoom + previous/next |
| Recent library and filename search | Yes | Yes |
| Bookmarks / favorites | Page bookmarks | Document favorites |
| In-document text search | Not yet | Yes |
| PDF text-note annotation | Not yet | Yes |
| Local sharing | Android Sharesheet | iOS ShareLink |
| Account or backend required | No | No |

There are **no demo documents** or third-party CDN dependencies at runtime. Build dependencies are resolved by Gradle/Xcode tooling, then packaged locally with the apps.

## Android

Requirements: Android Studio with Android SDK 35, JDK 17 and Gradle 8.11.1.

Open the repository root as a Gradle project, or run:

```sh
gradle :android:testDebugUnitTest :android:assembleDebug
```

Install `android/build/outputs/apk/debug/android-debug.apk` with your device tools. The application requests no Internet permission. Files are selected through Android's Storage Access Framework. The system PDF renderer rasterizes pages on-device; advanced editing and OCR are not yet included.

## iOS

Requirements: macOS, Xcode with iOS 16+ support and [XcodeGen](https://github.com/yonaskolb/XcodeGen) (build-time only, no runtime SDK).

```sh
cd ios
xcodegen generate
xcodebuild build -project Folqio.xcodeproj -scheme Folqio -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO
```

The generated Xcode project is intentionally gitignored; `ios/project.yml` is the source of truth. PDFs are imported from Files and stored in app-private Application Support. An Apple development account is needed for signing/distribution.

## Architecture and quality

- Design tokens: [branding/BRAND.md](branding/BRAND.md)
- Threat model and privacy: [docs/SECURITY.md](docs/SECURITY.md)
- Feature plan and QA gates: [docs/ROADMAP.md](docs/ROADMAP.md)
- CI: [Native CI](.github/workflows/ci.yml)

The rendered concept images are design references. Achieving 1:1 screen parity requires iterative real-device UI comparison and cannot be inferred solely from the artwork.

## License

MIT — see [LICENSE](LICENSE).
