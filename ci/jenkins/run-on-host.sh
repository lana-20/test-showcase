#!/usr/bin/env bash
# Host side of the Appium Java job: the emulator is already booted (by
# android-emulator-runner); start Appium here, then run the Jenkinsfile in
# Jenkinsfile Runner on the host network so it can reach Appium.
set -euo pipefail
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO"
mkdir -p out && chmod 777 out
adb devices
"$REPO/ci/android/quiet-system-dialogs.sh"   # no launcher ANR dialog over the app
nohup appium --address 127.0.0.1 --port 4723 --log-no-colors --log out/appium.log >/dev/null 2>&1 &
for _ in $(seq 1 60); do curl -fsS http://127.0.0.1:4723/status >/dev/null 2>&1 && break; sleep 1; done
curl -fsS http://127.0.0.1:4723/status
mkdir -p "$HOME/.m2"
# Jenkinsfile Runner runs as root, and Allure writes owner-only files: give
# everything back to the runner user so the results can be uploaded.
trap 'sudo chown -R "$(id -u):$(id -g)" "$REPO/out" "$REPO/suites/appium-java" "$HOME/.m2" 2>/dev/null || true' EXIT
# The repo is mounted twice: at /workspace, where the launcher looks for the
# Jenkinsfile, and at its host path, so the APK path the tests hand to Appium
# (which runs on the host) means the same file on both sides.
docker run --rm --network host \
  -v "$REPO:/workspace" -v "$REPO:$REPO" -v "$HOME/.m2:/root/.m2" \
  -e APP_PATH="$REPO/apps/ApiDemos-debug.apk" \
  -e HOST_CI_NAME="GitHub Actions" -e HOST_RUN_URL="${RUN_URL:-}" -e GIT_COMMIT="${GITHUB_SHA:-}" \
  "${JENKINS_IMAGE:?set JENKINS_IMAGE}"     # the launcher runs /workspace/Jenkinsfile
