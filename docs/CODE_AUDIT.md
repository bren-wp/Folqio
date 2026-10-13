# Folqio source audit — v0.1.3

## Confirmed cleanups

- Removed unused Android `MainActivity` imports and the superseded one-image wrapper overload.
- Refactored duplicate document-library JSON encoding into `saveRecent`.
- Removed hidden data-loss behavior: the recent-file limit now applies only to externally imported shortcuts, not generated PDFs.
- App-created PDFs are only deleted within their validated FileProvider UUID sandbox, after confirmation; external imported files are left untouched.
- Invalid iOS local deletion no longer causes metadata to be erased despite filesystem errors.
- Recycle replaced PDF previews and library thumbnails, size main PDF render by screen resolution.
- Clamp restored PDF page position when file page counts have changed.
- Enabled optimized Android release shrinking and automated lint/test/bundle checks; added required Android API / Apple Xcode baseline checks.

## Checks and limitations

`scripts/audit_source.py` checks duplicate imports, repeated class/function declarations in the same source file, accidental manifest permissions, build version consistency and selected shipping invariants. This **does not** constitute a perfect semantic dead-code audit; compiler/lint, device coverage and ongoing code review remain necessary.

No line of code or UI screen is considered verified as pixel-perfect until device screenshot overlay tests pass. OCR, scanner, signature and redaction parity are still incomplete.

## v0.1.7 validated Android imports

- PDFs selected from Android providers are copied to UUID-addressed private storage after validating the signature and the PdfRenderer page count; originals stay unchanged.
- A 150 MB cap bounds import resource consumption and incomplete files are deleted.
- Unneeded persisted provider read grants are removed from PDF picker and merge flows.
- Pure policy tests cover PDF header checks, safe document naming and import limits.
- Targeted source cleanup is not proof of fully eliminated dead code or pixel-perfect design parity.

## v0.1.8 audited visual tokens and executed simulator QA

- Consolidated repeated Android dark/mint palette literals into `FolqioPalette` used across Reader, Home and Settings. This eliminates 14 duplicate color declarations.
- CI no longer treats iOS test **compilation** as a successful test **execution**; simulator boot and XCTest run are now mandatory, including release checks.
- Real Android screenshots can be captured through a manually invoked GitHub workflow with visual navigation assertions. Only authentic PNGs may be included in the public README.
- A full semantic dead-code audit and pixel overlay verification require additional device instrumentation; code compiles, static checks and scoped source review do not prove either automatically.

## v0.1.9 annotated copy audit

- ReaderView no longer calls PDFKit `write(to:)` on the source PDF for notes and highlights. The new shared annotation-copy operation duplicates the PDF representation before modifying the selected page and persists through the existing protected library writer.
- A shared sheet composer avoids overlapping SwiftUI alert presentations for note and text-overlay entry.
- XCTest regressions cover isolation of source annotations and the equality of original file bytes after saving a new annotated copy.
- The version audit no longer hardcodes an obsolete version. These targeted changes are not a claim of a complete semantic dead-code audit or full-device visual parity.
