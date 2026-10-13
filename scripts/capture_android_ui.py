#!/usr/bin/env python3
"""Record authentic screens from the running Android app, never draw fake mockups."""
import hashlib
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

out = pathlib.Path("docs/media")
out.mkdir(parents=True, exist_ok=True)

def adb(*args, output=False):
    command = ["adb", *args]
    if output:
        return subprocess.check_output(command)
    subprocess.run(command, check=True)

def interface():
    adb("shell", "uiautomator", "dump", "/sdcard/folqio-view.xml")
    return ET.fromstring(adb("exec-out", "cat", "/sdcard/folqio-view.xml", output=True).decode())

def tap_navigation(label):
    root = interface()
    matches = []
    for item in root.iter("node"):
        if label not in (item.attrib.get("text"), item.attrib.get("content-desc")):
            continue
        bounds = re.findall(r"\[(\d+),(\d+)\]", item.attrib.get("bounds", ""))
        if len(bounds) != 2:
            continue
        (left, top), (right, bottom) = [(int(x), int(y)) for x, y in bounds]
        matches.append(((top + bottom) // 2, (left + right) // 2))
    if not matches:
        raise RuntimeError(f"Navigation control not found: {label}")
    y, x = max(matches)  # lowest matching control is the bottom navigation bar
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(2.5)

def screenshot(key):
    filename = out / f"android-{key}.png"
    filename.write_bytes(adb("exec-out", "screencap", "-p", output=True))
    raw = filename.read_bytes()
    if len(raw) < 10000 or not raw.startswith(b"\x89PNG\r\n\x1a\n"):
        raise RuntimeError(f"Invalid screenshot: {filename}")
    return hashlib.sha256(raw).hexdigest()

time.sleep(4)
digests = {"home": screenshot("home")}
for name in ("Files", "Favorites", "Settings"):
    tap_navigation(name)
    if name == "Settings":
        labels = {node.attrib.get("text", "") for node in interface().iter("node")}
        if not any("Reading & documents" in label or "Privacy & information" in label for label in labels):
            raise RuntimeError("Settings screen did not open; refusing misleading screenshot")
    digests[name.lower()] = screenshot(name.lower())
if len(set(digests.values())) != len(digests):
    raise RuntimeError("Duplicate images detected; navigation may not have worked")
print("Captured distinct, real Folqio screens:", ", ".join(digests))
