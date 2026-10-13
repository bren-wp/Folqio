# Folqio development and release gates

## Implemented in the 0.1.x branch

- Kotlin/Compose Android PDF reading and a recent/favorite library.
- On-device camera/photo-to-PDF creation (up to ten selected photos per PDF) via Android system activities, without Folqio camera or Internet app permissions.
- Android single-page extract/rotate as rasterized copies with an explicit disclosure.
- SwiftUI/PDFKit iOS PDF reader, local file import, text search, notes and highlighting search results.
- iOS VisionKit multi-page camera scanner, up to ten photos per PDF, merge, page rotation, reorder and extraction.
- iOS per-page offline OCR with Apple Vision, selectable recognized text and explicit copy (not yet searchable text-layer insertion).
- Shared Folqio design tokens, click-accessible quick actions and platform navigation.
- CI for Android tests/APK and iOS compilation, plus simulator tests when a runtime is available.

## Not completed (do not market as shipped)

1. True lossless Android PDF editing, page merge/reorder/split and forms support.
2. Android document edge detection, camera preview, perspective correction and multi-page scanning.
3. Privacy-audited Android offline OCR engine, plus document-wide full text indexing on both platforms.
4. Visual annotation toolbox parity, undo/redo, electronic and cryptographic signature flows.
5. Secure irreversible redaction and document sanitization.
6. Protected document vault, encryption-at-rest policy audit and biometric access controls.
7. 1:1 screenshot parity against all previous mockups across Android/iOS form factors.
8. Accessibility audit, very large/encrypted PDF handling, performance profiling and crashes.
9. Device-farm smoke tests, signed distribution, App Store and Google Play approvals.

## Release conditions

**Code compilation is not device QA.** The repository's GitHub Actions verify basic builds and unit-test availability, not pixel parity or a full security audit. Never publish as a complete editor before functionality is real and tested. Keep release candidates clearly labeled development builds and do not present a debug APK as Play Store ready.
