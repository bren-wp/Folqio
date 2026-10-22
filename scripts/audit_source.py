#!/usr/bin/env python3
"""Repository-wide Android audit.

Fatal checks: release/Play invariants and duplicate imports/declarations.
Advisories: lexical dead-code candidates and unused resources. Reflection,
Compose, Android manifest registration and generated bindings make a single
name occurrence insufficient proof that a declaration can be removed.
"""
from collections import Counter
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
ANDROID = ROOT / "android"
SOURCE = ANDROID / "src"
RESOURCES = SOURCE / "main" / "res"
NS = "{http://schemas.android.com/apk/res/android}"
ERRORS = []
ADVISORIES = []


def scan_symbols(texts):
    """Return conservative advisory suggestions, never destructive actions."""
    whole = "\n".join(texts.values())
    advice = []
    for path, source in texts.items():
        body = re.sub(r"^import[^\n]+$", "", source, flags=re.M)
        # A Kotlin alias/wildcard is not a normal unused import.
        for match in re.finditer(r"^import\s+([\w.]+)\s*$", source, re.M):
            name = match.group(1).rsplit(".", 1)[-1]
            if not re.search(r"\b" + re.escape(name) + r"\b", body):
                advice.append(f"{path}: possibly unused import {match.group(1)}")
        # Private functions may be referenced by tests, callbacks, or reflection.
        for match in re.finditer(
            r"^[ \t]*private[ \t]+(?:(?:suspend|inline|tailrec)[ \t]+)?"
            r"fun[ \t]+([A-Za-z_]\w*)[ \t]*\(", source, re.M
        ):
            name = match.group(1)
            if len(re.findall(r"\b" + re.escape(name) + r"\b", whole)) == 1:
                line = source.count("\n", 0, match.start()) + 1
                advice.append(f"{path}:{line}: private function {name} has no lexical call")
    return advice


def check_unique_imports_and_types(texts, errors):
    for path, source in texts.items():
        imports = re.findall(r"^import\s+([A-Za-z_][\w.*]*)\s*$", source, re.M)
        if len(imports) != len(set(imports)):
            errors.append(f"{path}: duplicate imports")
        # Only inspect non-indented top-level types; nested names may repeat
        # without colliding. Kotlin overloads are not duplicate methods.
        top_types = re.findall(
            r"^(?:(?:public|internal|private|data|sealed|open|abstract)\s+)*"
            r"(?:class|object|interface|enum\s+class)\s+([A-Za-z_]\w*)",
            source, re.M
        )
        for name, count in Counter(top_types).items():
            if count > 1:
                errors.append(f"{path}: duplicated top-level type {name}")


def audit_resources(resource_root, texts):
    """Review static references; exclude dynamic lookups from automatic removal."""
    referenced = "\n".join(texts.values())
    for path in resource_root.rglob("*"):
        if not path.is_file() or path.suffix.lower() not in (".xml", ".png", ".webp", ".jpg"):
            continue
        if path.parent.name.startswith("values") or path.name in ("ic_launcher.xml",):
            continue
        name = path.stem
        type_name = path.parent.name.split("-")[0]
        candidates = (
            rf"\bR\.{re.escape(type_name)}\.{re.escape(name)}\b",
            rf"@{re.escape(type_name)}/{re.escape(name)}\b",
        )
        if not any(re.search(token, referenced) for token in candidates):
            ADVISORIES.append(
                f"{path.relative_to(ROOT)}: resource lacks a lexical reference "
                "(check manifest, overlays and generated access before deletion)"
            )


def check_release_baseline():
    if (ROOT / "ios").exists():
        ERRORS.append("Removed iOS source directory has reappeared")
    gradle = (ANDROID / "build.gradle.kts").read_text(encoding="utf-8")
    manifest = (SOURCE / "main/AndroidManifest.xml").read_text(encoding="utf-8")
    ci = (ROOT / ".github/workflows/ci.yml").read_text(encoding="utf-8")
    release = (ROOT / ".github/workflows/release.yml").read_text(encoding="utf-8")
    for required in (
        "compileSdk = 36", "targetSdk = 36",
        'applicationId = "com.brendigo.folqio"',
        "isMinifyEnabled = true", "isShrinkResources = true",
    ):
        if required not in gradle:
            ERRORS.append("Missing Android release invariant: " + required)
    name = re.search(r'versionName\s*=\s*"(\d+\.\d+\.\d+)"', gradle)
    code = re.search(r'versionCode\s*=\s*(\d+)', gradle)
    if not name or not code or int(code.group(1)) < 15:
        ERRORS.append("Android versionCode must be >= 15 and versionName semantic")
    if any(t in ci + release for t in ("macos-", "  ios:", "needs: [android, ios]")):
        ERRORS.append("CI/release must not include obsolete iOS work")
    for workflow, content in (("CI", ci), ("release", release)):
        for gate in ("testDebugUnitTest", "lintDebug", "bundleRelease"):
            if gate not in content:
                ERRORS.append(f"{workflow} missing {gate} gate")
    try:
        doc = ET.fromstring(manifest)
        banned = {
            "android.permission.INTERNET", "android.permission.CAMERA",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
        }
        declared = {e.get(NS + "name") for e in doc.findall("uses-permission")}
        if declared & banned:
            ERRORS.append("Manifest adds unnecessary privileged permissions: " + str(declared & banned))
        app = doc.find("application")
        if app is None:
            ERRORS.append("Manifest missing application")
        else:
            if app.get(NS + "allowBackup") != "false":
                ERRORS.append("App backup unexpectedly enabled")
            if app.get(NS + "dataExtractionRules") != "@xml/data_extraction_rules":
                ERRORS.append("Data extraction rules missing")
            if not any(
                p.get(NS + "name") == "androidx.core.content.FileProvider"
                and p.get(NS + "authorities") == "${applicationId}.files"
                and p.get(NS + "exported") == "false"
                for p in app.findall("provider")
            ):
                ERRORS.append("Secure FileProvider missing")
    except ET.ParseError as exc:
        ERRORS.append("Invalid AndroidManifest.xml: " + str(exc))
    for path in (
        "docs/PRIVACY_POLICY.md", "docs/STORE_READINESS.md",
        "docs/GOOGLE_PLAY_LISTING.md",
        "android/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    ):
        if not (ROOT / path).is_file():
            ERRORS.append("Missing shipping resource: " + path)
    # Non-overlapping user-facing declarations are still reviewed manually.
    # A source audit cannot infer runtime state, R8 reachability or actual Play acceptance.


def main():
    sources = list(SOURCE.rglob("*.kt"))
    texts = {
        p.relative_to(ROOT).as_posix(): p.read_text(encoding="utf-8")
        for p in sorted(sources)
    }
    check_unique_imports_and_types(texts, ERRORS)
    ADVISORIES.extend(scan_symbols(texts))
    xml_texts = {
        p.relative_to(ROOT).as_posix(): p.read_text(encoding="utf-8")
        for p in sorted(RESOURCES.rglob("*.xml"))
    }
    audit_resources(RESOURCES, {**texts, **xml_texts,
                                "AndroidManifest": (SOURCE / "main/AndroidManifest.xml").read_text()})
    check_release_baseline()

    for issue in ADVISORIES:
        print("AUDIT REVIEW:", issue)
    if ERRORS:
        for issue in ERRORS:
            print("AUDIT FAILED:", issue, file=sys.stderr)
        return 1
    print(
        f"Android audit PASSED: {len(texts)} Kotlin sources (main+tests), "
        f"{len(xml_texts)} resource XML files, "
        f"{len(ADVISORIES)} advisory candidate(s). "
        "A lexical scan does not prove complete dead-code removal or 1:1 visual parity."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
