<div align="center">
  <img src="branding/folqio-symbol.svg" alt="Folqio logo" width="132" />
  <h1>Folqio</h1>
  <p><strong>Your documents. Your device.</strong></p>
  <p>Offline-first PDF reader and document tools for Android, by Brendigo.</p>
</div>

## What Folqio currently does on Android

- Import selected PDF documents into private app storage, open and navigate pages, zoom and resume reading.
- Organize documents with recent files, name search, favorites, bookmarks and display-name changes.
- Create PDFs from photos selected with the Android system picker or captured using the system camera.
- Merge two to five PDFs (up to 50 pages), and extract or rotate individual pages into **new, rasterized PDF copies**.
- Save and share PDFs using Android system interfaces; export selected documents to a CRC-verified ZIP archive.
- Choose from 25 language options for parts of the interface. Translation coverage remains incomplete.
- Work without a Folqio account, document upload service, in-app advertisements or tracking SDK.

**Limitations:** Android merging/extraction/rotation flatten pages into images: selectable text, vector objects, forms, annotations and signatures may not survive. Folqio is **not** a secure-redaction tool. Offline OCR, modification of existing PDF text, lossless page editing, automatic edge detection and multipage camera scanning are **not currently supported**. Do not describe them as existing features in Google Play.

## Actual Android screenshot

![Actual Android home screen captured on an emulator](docs/media/android-home.png)

The screenshot is from a running emulator, not an illustration. Additional real-device screenshots and accessibility verification are outstanding.

## Privacy and distribution

Folqio stores imported and generated PDFs locally. It has no Android Internet, camera, or broad-storage app permission: Android system activities handle optional camera and file selection. Only user-directed exporting or sharing transfers documents to other apps.

Review the [privacy policy](docs/PRIVACY_POLICY.md), [security notes](docs/SECURITY.md), [Google Play submission gate](docs/STORE_READINESS.md), [build instructions](docs/BUILDING.md) and [roadmap](docs/ROADMAP.md).

The [GitHub releases](https://github.com/bren-wp/Folqio/releases) currently contain **development artifacts only**. A debug APK and an unsigned AAB must never be represented as production-ready Google Play binaries.

Folqio is an Android-only Kotlin / Jetpack Compose project by **Brendigo**, licensed under [MIT](LICENSE).
