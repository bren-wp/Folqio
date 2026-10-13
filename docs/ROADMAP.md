# Folqio development and release gates

## Implemented in the 0.1.x branch

- Kotlin/Compose Android PDF reading and a recent/favorite library.
- On-device camera/photo-to-PDF creation (up to ten selected photos per PDF) via Android system activities, without Folqio camera or Internet app permissions.
- Android single-page extract/rotate as rasterized copies; additionally merge 2–5 PDFs with a 50-page safety cap, also rasterized.
- SwiftUI/PDFKit iOS PDF reader, local file import, text search, notes and highlighting search results.
- iOS VisionKit multi-page camera scanner, up to ten photos per PDF, merge, page rotation, reorder and extraction.
- iOS per-page or whole-document (up to 25 pages) offline OCR with Apple Vision, selectable recognized text, progress/cancel and explicit copy; no PDF text-layer insertion.
- Shared Folqio design tokens, click-accessible quick actions and platform navigation.
- CI for Android tests/APK and iOS compilation, plus simulator tests when a runtime is available.

## Not completed (do not market as shipped)

1. True lossless Android PDF editing/merging, page reorder/split and forms support.
2. Android document edge detection, camera preview, perspective correction and multi-page scanning.
3. Privacy-audited Android offline OCR engine, plus document-wide full text indexing on both platforms.
4. Visual annotation toolbox parity, undo/redo, electronic and cryptographic signature flows.
5. Secure irreversible redaction and document sanitization.
6. Protected document vault, encryption-at-rest policy audit and biometric access controls.
7. 1:1 screenshot parity against all previous mockups across Android/iOS form factors.
8. Accessibility audit, very large/encrypted PDF handling, performance profiling and crashes.
9. Device-farm smoke tests, signed distribution, App Store and Google Play approvals.

## v0.1.3 hardening work

- Upgrade Android target/compile to API 36 and build tooling to AGP 8.13.2 / Gradle 8.13.
- Edge-to-edge system bars and safe drawing insets in reader; handle incoming PDFs on activity reuse.
- Bound bitmap allocations to screen size and recycle replaced preview/thumbnail buffers.
- Keep locally generated documents in the library beyond the external-shortcut limit.
- Explicit removal confirmation; delete only app-generated PDFs inside a validated sandbox path.
- Shared serialization for recent-document metadata; remove unused imports and unused helper overloads.
- Add Google Play signing inputs and native CI lint/AAB checks, plus Apple privacy manifest and deterministic 1024px icon generation.

**Open release blockers:** physical-device QA, full screenshot parity, production icon sets, actual signed Play AAB and signed iOS archive, App Store listing assets, privacy disclosures, accessibility audit and advanced PDF editor parity.

## Release conditions

**Code compilation is not device QA.** The repository's GitHub Actions verify basic builds and unit-test availability, not pixel parity or a full security audit. Never publish as a complete editor before functionality is real and tested. Keep release candidates clearly labeled development builds and do not present a debug APK as Play Store ready.
