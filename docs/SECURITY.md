# Folqio privacy and security model

## Privacy commitments in the current implementation

- No account system, analytics, telemetry SDK, network PDF service or Android `INTERNET` permission.
- Android document picker obtains user-selected read grants; optional camera capture invokes the **system camera** rather than requiring Folqio camera permission.
- Android created PDFs and camera photos are written to app-private `filesDir` and exposed to other applications only by the explicit `FileProvider` content URI flow.
- Android recent PDF references/favorites are stored in private preferences. Content provider grants may be revoked after external documents move or providers reset.
- iOS PDFs are imported to app-private Application Support with backup exclusions and file protection, subject to OS/device policy. Passwords used to unlock encrypted PDF files are not stored in the library.
- iOS VisionKit scanner and system Photos picker use platform UI. The OS may optionally obtain photos from device-synced sources, but **Folqio does not upload their contents to its own backend**.
- PDF rendering is handled locally by Android PdfRenderer and Apple PDFKit.
- iOS uses Apple's on-device Vision OCR for a page or up to 25 pages with cancel/progress; recognized text stays in view state until the user explicitly copies it. It does not create a searchable PDF text layer.
- Android does not yet include an OCR SDK. We explicitly did **not** bundle Google ML Kit because its Android SDK documentation reports collection of diagnostic identifiers and performance information, which needs a separate privacy assessment.
- Exports and share sheets are user-initiated.
- No runtime CDN. Gradle dependencies are fetched at **build time** and packaged into the APK; native iOS features use Apple frameworks.

## Threats and limitations

- PDFs and images are untrusted input. Platform parsers should be kept updated and fuzz/compatibility tested. A successful compile does not prove safety.
- Android image decode downsamples inputs to bound typical output memory and caps photo imports at ten files; PDF rendering is capped to 1200×3000 pixels.
- Android page copies and merged PDFs are rendered bitmaps: **not true redaction**, not full preservation of forms/text. Merge is capped at five input PDFs / fifty pages.
- iOS PDFPage operations may have edge cases with tagged PDFs, signed/encrypted documents, embedded media or unusual forms; do not claim complete fidelity without tests.
- iOS notes/highlights are written to the app's local copy. Preserve originals before destructive modifications.
- No cryptographic document vault, password-reset flow, signing, OCR, secure redaction or malware sanitization is claimed yet.
- Limit third-party libraries. Any new PDF engine needs a license audit, dependency/SBOM review, and security update strategy before adoption.
- Neither platform uses an account, notifications, analytics, ads or push registration.

## Release gate

Do not mark pixel-perfect parity, encrypted storage guarantees, or safe permanent redaction as complete without real device/security testing and evidence. Android debug signing is for development and is not production signing.

## v0.1.3 storage and distribution safeguards

- Deletion never follows an arbitrary persisted URI path: it only targets UUID-named PDFs under the app-owned `created/` directory after comparing the URI authority and FileProvider alias.
- Android never silently evicts created documents when the external recent-shortcut cap is reached.
- Android URI/filename metadata and reading positions remain in private app preferences; `allowBackup=false` is set.
- Apple privacy manifest is supplied as a starting declaration. The final approved-reason API and App Store privacy questionnaire must still be audited against the production binary.
- Debug APK/AAB from CI must **not** be used as a production signed Play upload. No private upload key is stored in Git.
