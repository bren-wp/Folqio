#!/usr/bin/env python3
"""Android-only Folqio hygiene and Google Play baseline checks. Not store certification."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
errors = []
candidates = []
android = root / "android"
kotlin_files = list(android.rglob("*.kt"))

if (root / "ios").exists():
    errors.append("iOS source tree is not allowed in Android-only Folqio")
if (root / "scripts/run-ios-tests.sh").exists():
    errors.append("Obsolete iOS tests script remains")

gradle = (android / "build.gradle.kts").read_text(encoding="utf-8")
manifest_text = (android / "src/main/AndroidManifest.xml").read_text(encoding="utf-8")
ci = (root / ".github/workflows/ci.yml").read_text(encoding="utf-8")
release = (root / ".github/workflows/release.yml").read_text(encoding="utf-8")
for token in ("compileSdk = 36", "targetSdk = 36", "applicationId = \"com.brendigo.folqio\"",
              "isMinifyEnabled = true", "isShrinkResources = true"):
    if token not in gradle:
        errors.append("Android build invariant missing: " + token)

version_name = re.search(r'versionName\s*=\s*"([0-9]+\.[0-9]+\.[0-9]+)"', gradle)
version_code = re.search(r'versionCode\s*=\s*(\d+)', gradle)
if not version_name or not version_code or int(version_code.group(1)) < 14:
    errors.append("Android release version must use semver name and monotonically increasing code")

ns = "{http://schemas.android.com/apk/res/android}"
try:
    manifest = ET.fromstring(manifest_text)
    declared = [x.get(ns + "name", "") for x in manifest.findall("uses-permission")]
    prohibited = {"android.permission.INTERNET", "android.permission.CAMERA",
                  "android.permission.READ_EXTERNAL_STORAGE", "android.permission.READ_MEDIA_IMAGES",
                  "android.permission.READ_MEDIA_VIDEO", "android.permission.MANAGE_EXTERNAL_STORAGE"}
    if prohibited.intersection(declared):
        errors.append("Unexpected broad/network/camera permission: " + str(prohibited.intersection(declared)))
    app = manifest.find("application")
    if app is None:
        errors.append("Android application manifest entry missing")
    else:
        if app.get(ns + "allowBackup") != "false":
            errors.append("Android cloud backup must remain disabled")
        if app.get(ns + "dataExtractionRules") != "@xml/data_extraction_rules":
            errors.append("Android device-transfer policy missing")
        providers = app.findall("provider")
        if not any(x.get(ns + "name") == "androidx.core.content.FileProvider"
                   and x.get(ns + "exported") == "false"
                   and x.get(ns + "authorities") == "${applicationId}.files" for x in providers):
            errors.append("Non-exported app-owned FileProvider missing")
except ET.ParseError as exc:
    errors.append("Invalid AndroidManifest.xml: " + str(exc))

if "macos-" in ci or "  ios:" in ci or "  ios:" in release or "needs: [android, ios]" in release:
    errors.append("Active CI/release must be Android-only")
if "lintDebug" not in ci or "bundleRelease" not in ci or "testDebugUnitTest" not in ci:
    errors.append("Android CI verification gate is incomplete")
if "lintDebug" not in release or "bundleRelease" not in release or "testDebugUnitTest" not in release:
    errors.append("Development release Android verification gate is incomplete")
for relative in ("docs/PRIVACY_POLICY.md", "docs/STORE_READINESS.md",
                 "docs/GOOGLE_PLAY_LISTING.md", "android/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"):
    if not (root / relative).is_file():
        errors.append("Required app privacy/store resource missing: " + relative)

for path in kotlin_files:
    code = path.read_text(encoding="utf-8")
    imports = re.findall(r"^import\s+([A-Za-z_][\w.]*)\s*$", code, re.M)
    if len(set(imports)) != len(imports):
        errors.append(f"{path.relative_to(root)}: duplicate imports")
    types = re.findall(r"^[ \t]*(?:private\s+|internal\s+)?(?:data\s+)?(?:class|object|enum\s+class)\s+([A-Z]\w*)", code, re.M)
    if len(types) != len(set(types)):
        errors.append(f"{path.relative_to(root)}: duplicate type declarations")
    body = re.sub(r"^import[^\n]+$", "", code, flags=re.M)
    for imp in imports:
        short = imp.rsplit(".", 1)[-1]
        if not re.search(r"\b" + re.escape(short) + r"\b", body):
            candidates.append(f"{path.relative_to(root)}: possibly unused import {imp}")

if errors:
    for issue in errors:
        print("AUDIT FAILED:", issue, file=sys.stderr)
    sys.exit(1)
for candidate in candidates:
    print("AUDIT REVIEW:", candidate)
print(f"Android/Google Play source baseline passed ({len(kotlin_files)} Kotlin files; "
      f"{len(candidates)} advisory candidate(s)). This does not certify Play approval.")
