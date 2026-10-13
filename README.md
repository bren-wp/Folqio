# FOLQIO — PDF Reader & Editor

**Your documents. Your device.**

Folqio is a privacy-oriented, offline-first document app with separately implemented **Android (Kotlin/Jetpack Compose)** and **iOS (Swift/SwiftUI/PDFKit)** versions. The UI uses Folqio's navy, slate and mint visual language.

> **Status: native development preview, v0.1.1. Not production-ready.** All examples shown in previous concept renders are targets, not proof of implemented screens or pixel-perfect parity. There are no demo PDF files or user accounts in the app.

## Real functionality

| Feature | Android | iOS |
| --- | --- | --- |
| PDF import and reading | System picker + PdfRenderer | System Files picker + PDFKit |
| Page navigation and zoom | Yes | Yes |
| Local document library / name search | Recent PDFs | Copied into private app storage |
| Favorites | Yes | Yes |
| Rename library display label | Yes | Yes |
| Open encrypted PDF with password | Not yet | Yes; entered locally and not persisted |
| Page bookmarks | Yes | No |
| PDF full-text search | No | Yes |
| Offline OCR on one selected PDF page | Not yet | Yes, on-device Apple Vision (read/copy only) |
| PDF annotations | No | Text notes; highlight first search result |
| Camera scanning | System camera, single photo into PDF; manual cropping not implemented | VisionKit camera scan, multi-page, auto edge adjustment |
| Photo-to-PDF | Up to ten photos per multipage PDF | Up to ten photos per multipage PDF |
| Merge multiple PDFs | Not yet | Yes, from local library |
| Extract, rotate, reorder | Extract/rotate current page to **flattened** copy; no reorder | Extract, rotate, reorder in a new vector-preserving PDF |
| Share PDF | Android Sharesheet | iOS ShareLink |
| Internet/account needed for PDF operations | No | No |

### Important limitations

- Android's current PDF engine is read-only. Its export operations create **rasterized** page copies; they do not preserve searchable text, forms or native annotations. Do not use those operations as secure redaction.
- Android camera capture does not yet replicate the reference scanner's edge correction, multi-page capture and filters.
- iOS PDF page operations retain PDF content rather than rendering to a bitmap, but complex PDF fidelity and forms require a broader compatibility test suite.
- No Android OCR, document-wide OCR indexing, encryption vault, text reflow, direct editing of original PDF text, creating password-protected PDFs, verified redaction, undo/redo editor, or signing workflow has been shipped yet.
- Full 1:1 visual matching to prior image renders requires real-device screenshots and audited interaction states. Current screens follow the same brand rather than claiming pixel-level equivalence.

## Development

Android requirements: Android SDK 35, JDK 17, Gradle 8.11.1.

```sh
gradle :android:testDebugUnitTest :android:assembleDebug
```

Android APK (debug): `android/build/outputs/apk/debug/android-debug.apk`, uploaded as a CI artifact.

iOS requirements: Xcode, iOS 16+, [XcodeGen](https://github.com/yonaskolb/XcodeGen) as a build-time tool.

```sh
xcodegen generate --spec ios/project.yml --project ios
xcodebuild build -project ios/Folqio.xcodeproj -scheme Folqio -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO
```

Build tooling downloads pinned dependencies at development/build time, and packages the runtime code into the apps. It does not fetch scripts, fonts or PDF processing code from a CDN while the app runs. The iOS app relies on Apple's on-device frameworks. Android uses system PDF/camera APIs.

## Project docs

- [Brand and design tokens](branding/BRAND.md)
- [Design parity QA checklist](docs/DESIGN_QA.md)
- [Manual interaction test checklist](docs/TESTING.md)
- [Security and privacy](docs/SECURITY.md)
- [Roadmap and release criteria](docs/ROADMAP.md)

MIT license — see [LICENSE](LICENSE).
