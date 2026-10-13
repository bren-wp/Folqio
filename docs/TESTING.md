# Functional smoke test checklist

Use personal test documents with no real sensitive information. Test on physical devices, not only CI.

## Android

1. Launch with no files: verify empty state, no demo PDFs, no account.
2. Open a multi-page PDF from Files; navigate and zoom, return and reopen.
3. Mark current PDF as favorite, switch tabs, rename its display label, search its filename.
4. Tap Scan and take a photo with the **system camera**; verify that a one-page PDF opens.
5. Tap Photo and choose an image; verify a separate PDF is saved locally.
6. Extract and rotate page copies; verify the new PDF opens, and disclosure says **flattened**.
7. Share a PDF, then return; confirm no sharing happens without explicit user action.
8. Grant revocation and corrupt PDF input: verify graceful error and no crash.
9. No-network test: airplane mode, reopen app, repeat offline actions.

## iOS

1. Open a multi-page PDF from Files, and confirm it persists after app restart.
2. Search text, highlight a found selection, close/reopen and verify annotation.
3. Add text note to a page, save, reopen.
4. Select camera Scan on a device with a camera, capture multiple pages; verify page count.
5. Choose Photo from Photos picker; verify it becomes a local PDF.
6. Select 2+ library PDFs in Merge, verify total pages and originals.
7. Page tools: select a page, rotate, move and extract as **new copies**.
8. Toggle favorite, rename display label, switch Home/Files/Favorites tabs and filter names.
9. Open an encrypted PDF with correct and incorrect passwords; verify cancel, unlock and no saved password. Attempt unsupported/corrupted inputs; verify clear error messages.
10. Test with the phone offline and verify no application-hosted document upload.

## Known gaps

Android currently has no text-level PDF editing or OCR, and its capture is single photo without auto-edge correction. iOS device/simulator screenshots are not yet compared pixel-by-pixel to the reference art. Production security and App Store/Play Store acceptance are not tested.
