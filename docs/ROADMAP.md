# Folqio roadmap

## v0.1.0 — first native baseline (this PR)

- Android Kotlin/Compose project with on-device PDF rendering, system picker, recent files, page navigation, bookmarks, and sharing.
- iOS SwiftUI/PDFKit project with local import, readable PDFs, search, favorites, note annotations, and sharing.
- Brand tokens and vector source files; no bundled sample documents, external CDN, telemetry or account dependency.
- Android and iOS CI build checks.

## Next milestones (not yet implemented)

1. Harden state restoration and document-provider permission recovery; Android page thumbnails and in-document search.
2. Full document scanner with local camera capture and perspective correction on both platforms.
3. Offline OCR models bundled in the binary, page merge/split/reorder/rotate/extract operations.
4. Advanced annotations, undo/redo, form support, signatures, verified redaction and secure exports.
5. Pixel-level screenshot comparison to Folqio reference visuals with Android/iOS screen-size matrices and accessibility review.
6. Signed release builds, app icons, production crash/performance smoke testing, App Store / Play Store submissions.

## Definition of done

Each feature requires executable behavior, persistent changes, error handling, permission handling, automated tests where meaningful, and testing on both device platforms. Mockups are visual references, not proof of functionality.
