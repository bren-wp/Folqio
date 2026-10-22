# Folqio — Android privacy and security

Folqio is an offline-first Android app. Its Android manifest does not request INTERNET, CAMERA, READ_MEDIA_IMAGES, MANAGE_EXTERNAL_STORAGE or broad file permissions. The system photo/document picker and camera app handle user-initiated imports and capture. No account, ad SDK or tracker is included.

Imported PDFs are copied to app-private storage, with a 150 MB stream cap and PDF signature/readability checks. Photo imports have size limits and bounded decoding. Share destinations are user-selected through Android system intents; Folqio does not upload documents to its own servers.

The app uses a non-exported FileProvider to grant narrow content URI access. Local library metadata, bookmarks, favorites and preferences use private Android SharedPreferences. Cloud backup and device transfer of Folqio app data are disabled in the manifest/configuration; copies exported to other apps are outside Folqio's control.

App-created document deletion uses UUID filename checks, local tombstone staging and metadata rollback. In a collision where a live PDF and tombstone both exist, both copies remain in private storage; a user-facing recovery UI is still needed. Running out of storage or force-stopping during deletion requires physical-device testing.

PDF/image parsers process untrusted inputs and require ongoing fuzzing and OS updates. Rasterized page operations are not lossless, not reliable for signatures/forms and **not secure redaction**. No encrypted vault, password manager, local offline OCR engine or signature-verification feature is claimed.

Keep upload keystores, aliases/passwords and signing credentials outside repository code and public artifacts. Review resolved transitive dependencies and packaged native libraries before store submission.
