#!/usr/bin/env bash
set -euo pipefail

# Run the real XCTest bundle instead of reporting a compiled-only test suite.
# Pick an available iPhone (prefer an already booted simulator).
DEVICE_ID="$(xcrun simctl list devices available -j | python3 -c '
import json, sys
devices = [device for group in json.load(sys.stdin)["devices"].values()
           for device in group
           if "iPhone" in device.get("name", "")
           and device.get("state") in ("Shutdown", "Booted")]
devices.sort(key=lambda device: (device.get("state") != "Booted", device.get("name", "")))
print(devices[0]["udid"] if devices else "")
')"
if [ -z "$DEVICE_ID" ]; then
  echo "::error::No available iPhone simulator. Device-test execution is required."
  exit 1
fi

if ! xcrun simctl list devices booted -j | grep -q "$DEVICE_ID"; then
  xcrun simctl boot "$DEVICE_ID"
fi
xcrun simctl bootstatus "$DEVICE_ID" -b
# Always keep a real xcresult bundle. On failures expose the XCTest assertion
# details in Actions logs and retain the result for reproducible diagnosis.
RESULT_BUNDLE="${RUNNER_TEMP:-/tmp}/FolqioTests.xcresult"
rm -rf "$RESULT_BUNDLE"
if ! xcodebuild test \
  -project ios/Folqio.xcodeproj \
  -scheme Folqio \
  -destination "platform=iOS Simulator,id=$DEVICE_ID" \
  -parallel-testing-enabled NO \
  -resultBundlePath "$RESULT_BUNDLE" \
  CODE_SIGNING_ALLOWED=NO \
  -quiet; then
  echo "::error::Executed XCTest suite failed. Extracting failed test details."
  xcrun xcresulttool get test-results summary --path "$RESULT_BUNDLE" --format json || true
  xcrun xcresulttool get test-results tests --path "$RESULT_BUNDLE" --format json || true
  exit 1
fi
