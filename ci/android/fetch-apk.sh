#!/usr/bin/env bash
# Download the pinned ApiDemos APK (Appium's public sample app) and check its digest.
set -euo pipefail
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
VERSION="${APIDEMOS_VERSION:-v6.0.18}"
SHA256="${APIDEMOS_SHA256:-a9eecf37b26cd084855c530db81c2bb1b91f4c1b095a04f47aa7c20e2791f686}"
mkdir -p "$REPO/apps"
APK="$REPO/apps/ApiDemos-debug.apk"
curl -fsSL -o "$APK" "https://github.com/appium/android-apidemos/releases/download/$VERSION/ApiDemos-debug.apk"
echo "$SHA256  $APK" | sha256sum -c - 2>/dev/null || echo "$SHA256  $APK" | shasum -a 256 -c -
