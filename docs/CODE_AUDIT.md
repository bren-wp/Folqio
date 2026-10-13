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
