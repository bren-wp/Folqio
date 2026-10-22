# Folqio — Google Play release gate (October 2026)

**Scope: Android only.** This repository's development GitHub releases are not Google Play production submissions.

## Source/build requirements

- Android 16 (API 36) is required for new phone app submissions and updates from 31 August 2026; Folqio targets and compiles API 36.
- Application ID must remain com.brendigo.folqio. Increase versionCode for every accepted Play update (0.1.13 uses code 14).
- Pass Android JVM unit tests, source policy audit, Android lint, debug APK, release AAB, minification and resource shrinking.
- Check final AAB/APK for unexpected merged permissions, tracker SDKs and native .so dependencies. If native libraries are present, verify 16 KB page-size compatibility.
- Sign Play AAB with the correct **upload key**, enroll in Play App Signing, verify certificate fingerprint. Unsigned release AAB and debug APK are for development, not Play upload.
- Never commit upload keystores, passwords or private signing material to the public repository.

## Play Console / policy verification

- Complete truthful app description and metadata; advertise only Android features proven in code. Folqio is currently a PDF reader with document-management and flattened/rasterized page tools. Do **not** advertise OCR, lossless editing, secure redaction or automatic multipage scanning.
- Host the complete privacy policy at a public, active, non-geofenced, non-editable URL; add it to Play Console. In-app policy text exists, but hosting and Console linkage must be verified.
- Complete Data safety based on the final packaged app, data access, sharing destinations and SDK dependencies. Do not infer a Play Console declaration from source alone.
- Complete App content (ads, content rating, target audience, app access, legal disclosures), developer identity and any applicable country requirements in Play Console.
- Use an accurate Folqio icon, screenshots of the **running Android app**, valid feature graphic, contact details and tested support links.
- Test camera picker, photo picker, storage/full disk, file-provider grants, no-network use, API 26 and API 36, large PDFs, 16 KB devices if needed, TalkBack, larger fonts, screen rotation and background/foreground lifecycle.
- Google reviews the uploaded bundle and Console declarations independently; no build script can guarantee approval.

## Distribution status

- [x] Kotlin/Compose Android app and targetSdk 36
- [x] App-private FileProvider / minimal manifest permission design
- [x] In-app privacy text and development release signing disclosures
- [ ] Production AAB signed with verified upload key
- [ ] Current CI fully green after latest changes
- [ ] Public privacy policy URL verified and registered in Play Console
- [ ] Final Data safety, target audience, content rating and store metadata reviewed
- [ ] Physical-device + accessibility verification and Google Play review completed

References:
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://support.google.com/googleplay/android-developer/answer/10144311
- https://support.google.com/googleplay/android-developer/answer/9898842
- https://developer.android.com/guide/practices/page-sizes
