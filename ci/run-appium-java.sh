#!/usr/bin/env bash
# Appium Java (JUnit 6) against ApiDemos. Needs a running Appium server
# (APPIUM_URL, default http://127.0.0.1:4723) and a booted device or emulator.
SUITE=appium-java
source "$(dirname "$0")/lib/common.sh"
cd "$SUITE_DIR"
export APP_PATH="${APP_PATH:-$REPO/apps/ApiDemos-debug.apk}"
[ -f "$APP_PATH" ] || "$REPO/ci/android/fetch-apk.sh"
log "Running Appium Java suite"
set +e
# Gradle Wrapper: the pinned Gradle (checksum-verified) and, in the Jenkins
# runner image, every dependency are already in GRADLE_USER_HOME.
./gradlew test --no-daemon --console=plain -Dallure.results.directory="$RESULTS"
code=$?
set -e
finish $code
