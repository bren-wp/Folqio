#!/usr/bin/env python3
"""Small dependency-free hygiene and release-invariant audit."""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parent.parent
issues = []
sources = list((root / "android").rglob("*.kt")) + list((root / "ios").rglob("*.swift"))
for path in sources:
    content = path.read_text(encoding="utf-8")
    imports = re.findall(r"^import\s+([A-Za-z_][\w.]*)\s*$", content, re.M)
    if len(imports) != len(set(imports)):
        issues.append(f"{path.relative_to(root)}: duplicate imports")
    # Local duplicate declarations are unintended; overloaded functions with different
    # signatures are deliberately not flagged to avoid false positives.
    decls = re.findall(r"^\s*(?:private\s+|internal\s+)?(?:data\s+)?(?:class|object|enum\s+class)\s+([A-Z]\w*)", content, re.M)
    if len(decls) != len(set(decls)):
        issues.append(f"{path.relative_to(root)}: duplicate type declarations")

gradle = (root / "android/build.gradle.kts").read_text(encoding="utf-8")
manifest = (root / "android/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
ci = (root / ".github/workflows/ci.yml").read_text(encoding="utf-8")
project = (root / "ios/project.yml").read_text(encoding="utf-8")
for required in ("compileSdk = 36", "targetSdk = 36", "isMinifyEnabled = true", "isShrinkResources = true"):
    if required not in gradle:
        issues.append("Android build missing " + required)
for prohibited in ("android.permission.INTERNET", "READ_EXTERNAL_STORAGE", "MANAGE_EXTERNAL_STORAGE"):
    if prohibited in manifest:
        issues.append("Unexpected Android permission " + prohibited)
if "macos-26" not in ci or "bundleRelease" not in ci or "lintDebug" not in ci:
    issues.append("CI is missing store-compliance gates")
if not (root / "ios/generate_app_icon.swift").is_file():
    issues.append("App Store icon generator missing")
if not (root / "android/src/main/res/mipmap-anydpi-v26/ic_launcher.xml").is_file():
    issues.append("Android adaptive launcher icon missing")
if not (root / "ios/Folqio/PrivacyInfo.xcprivacy").is_file():
    issues.append("Apple privacy manifest missing")
else:
    import plistlib
    try:
        privacy = plistlib.loads((root / "ios/Folqio/PrivacyInfo.xcprivacy").read_bytes())
        allowed = {x["NSPrivacyAccessedAPIType"]: x["NSPrivacyAccessedAPITypeReasons"]
                   for x in privacy["NSPrivacyAccessedAPITypes"]}
        if "CA92.1" not in allowed.get("NSPrivacyAccessedAPICategoryUserDefaults", []):
            issues.append("Apple UserDefaults API reason missing")
        if "C617.1" not in allowed.get("NSPrivacyAccessedAPICategoryFileTimestamp", []):
            issues.append("Apple file timestamp API reason missing")
    except (OSError, ValueError, KeyError, TypeError) as exc:
        issues.append(f"Invalid Apple privacy manifest: {exc}")
android_name = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)
ios_name = re.search(r'MARKETING_VERSION:\s*"([^"]+)"', project)
android_code = re.search(r'versionCode\s*=\s*(\d+)', gradle)
ios_code = re.search(r'CURRENT_PROJECT_VERSION:\s*"(\d+)"', project)
if (not android_name or not ios_name or android_name.group(1) != ios_name.group(1)
        or not android_code or not ios_code or android_code.group(1) != ios_code.group(1)):
    issues.append("Platform versions are inconsistent")
if issues:
    print("\n".join("AUDIT FAILED: " + issue for issue in issues), file=sys.stderr)
    sys.exit(1)
print(f"Source audit passed for {len(sources)} Kotlin/Swift files; store baselines checked.")
