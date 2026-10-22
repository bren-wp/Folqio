# Folqio — Android-only roadmap

## Implemented and reviewed in source

- Native Kotlin/Jetpack Compose Android UI in Folqio Midnight Navy / Slate / Mint.
- PDF library, imports copied to local private files, reader navigation, zoom, favorites, bookmarks, name search, rename and last page.
- On-device photos-to-PDF through system camera and document selection, with 25 MB per-photo checks and bounded decode.
- PDF merge (two to five inputs, up to 50 pages), single-page extraction and rotation, all rasterized and saved separately.
- ZIP export with streaming byte limits, CRC verification, user-directed save/share.
- UUID-based staged deletion with rollback and startup recovery. In 0.1.13, live-file collisions preserve the pending copy instead of deleting it.
- Android API 36 target, R8/resource shrinking and automated JVM tests, lint and unsigned release build.

## Not implemented — do not market as supported

1. Lossless PDF merge/rotate/split/reorder; edit existing PDF text, forms, secure redaction or cryptographic signatures.
2. Offline OCR and searchable OCR output; multipage automatic-edge document scanner and perspective correction.
3. Fully translated user interface for all 25 language choices.
4. Device-farm testing, accessibility audit, large/encrypted PDF guarantees, low-storage recovery simulations and sustained performance profiling.
5. Signed production Play AAB, verified Play Console listing, Data safety form and production approval.
6. User-visible recovery flow for rare conflicting pending-delete files.

## Release gate

Pass Android JVM tests, source audit, lint, minified release AAB and manual device tests. Verify app behavior and publicly hosted privacy policy. Do not claim Google Play approval or professional lossless PDF editing until actually achieved.
