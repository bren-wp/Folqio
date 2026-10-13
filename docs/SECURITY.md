# Folqio privacy and security model

## Privacy commitments in the current implementation

- No account system, analytics, telemetry SDK, network PDF service or Android `INTERNET` permission.
- Android document picker obtains user-selected read grants; optional camera capture invokes the **system camera** rather than requiring Folqio camera permission.
- Android created PDFs and camera photos are written to app-private `filesDir` and exposed to other applications only by the explicit `FileProvider` content URI flow.
- Android recent PDF references/favorites are stored in private preferences. Content provider grants may be revoked after external documents move or providers reset.
- iOS PDFs are imported to app-private Application Support with backup exclusions and file protection, subject to OS/device policy. Passwords used to unlock encrypted PDF files are not stored in the library.
- iOS VisionKit scanner and system Photos picker use platform UI. The OS may optionally obtain photos from device-synced sources, but **Folqio does not upload their contents to its own backend**.
- PDF rendering is handled locally by Android PdfRenderer and Apple PDFKit.
- Exports and share sheets are user-initiated.
- No runtime CDN. Gradle dependencies are fetched at **build time** and packaged into the APK; native iOS features use Apple frameworks.

## Threats and limitations

- PDFs and images are untrusted input. Platform parsers should be kept updated and fuzz/compatibility tested. A successful compile does not prove safety.
- Android image decode downsamples inputs to bound typical output memory; PDF rendering is capped to 1200×3000 pixels.
- Android exported PDF pages are rendered bitmaps: **not true redaction**, not full preservation of forms/text.
- iOS PDFPage operations may have edge cases with tagged PDFs, signed/encrypted documents, embedded media or unusual forms; do not claim complete fidelity without tests.
- iOS notes/highlights are written to the app's local copy. Preserve originals before destructive modifications.
- No cryptographic document vault, password-reset flow, signing, OCR, secure redaction or malware sanitization is claimed yet.
- Limit third-party libraries. Any new PDF engine needs a license audit, dependency/SBOM review, and security update strategy before adoption.
- Neither platform uses an account, notifications, analytics, ads or push registration.

## Release gate

Do not mark pixel-perfect parity, encrypted storage guarantees, or safe permanent redaction as complete without real device/security testing and evidence. Android debug signing is for development and is not production signing.
