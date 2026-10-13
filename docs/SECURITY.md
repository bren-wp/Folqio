# Security and privacy architecture

## Privacy contract

- No sign-in, ads, analytics SDK, external document API, or `INTERNET` permission on Android.
- iOS imports only user-selected PDFs; files are copied into app-local Application Support.
- Android uses the system document picker and persistable read URI grants; no broad storage permission.
- Android recent-file metadata is stored in app-private preferences (without PDF contents).
- iOS file copies are excluded from backup; file protection applies where supported.
- Sharing happens only after the user explicitly invokes the system share sheet.
- PDF renderers are platform frameworks: Android PdfRenderer and Apple's PDFKit; documents are not sent to a server.
- Third-party build dependencies are version-pinned and packaged into the application, never loaded via CDN at runtime.

## Threat considerations

- A PDF may be malformed or hostile. Parsing remains a potential native attack surface. Keep OS/framework patches current.
- Android bitmap rendering is capped to 1200×3000 pixels to constrain memory usage; zoom scales the preview rather than allocating huge bitmaps.
- An Android document provider may revoke permission. Failure is displayed to the user rather than silently dropping records.
- The iOS file importer uses security-scoped URLs, copies selected PDFs into a private directory, and assigns generated filenames on disk.
- Neither app claims that overlaid black rectangles constitute secure redaction. Redaction is not yet implemented.
- OCR, signature verification, advanced editing, sanitization and attachment handling require separate, audited implementations.

## Release gates

Do not claim pixel-perfect parity or finished PDF editing until actual device QA validates those requirements.
Never publish production app bundles without signing, static analysis, review of third-party licenses and tested backups/export behavior.
Report vulnerabilities privately through the repository owner rather than public issue attachments containing sensitive documents.
