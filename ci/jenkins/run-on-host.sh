#!/usr/bin/env bash
# Host side of the Appium Java job: the emulator is already booted (by
# android-emulator-runner); start Appium here, then run the Jenkinsfile in
# Jenkinsfile Runner on the host network so it can reach Appium.
set -euo pipefail
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO"
mkdir -p out && chmod 777 out
adb devices
nohup appium --address 127.0.0.1 --port 4723 --log-no-colors --log out/appium.log >/dev/null 2>&1 &
for _ in $(seq 1 60); do curl -fsS http://127.0.0.1:4723/status >/dev/null 2>&1 && break; sleep 1; done
curl -fsS http://127.0.0.1:4723/status
mkdir -p "$HOME/.m2"
docker run --rm --network host \
  -v "$REPO:/workspace" -v "$HOME/.m2:/root/.m2" \
  -e HOST_CI_NAME="GitHub Actions" -e HOST_RUN_URL="${RUN_URL:-}" -e GIT_COMMIT="${GITHUB_SHA:-}" \
  "${JENKINS_IMAGE:?set JENKINS_IMAGE}" \
  -f /workspace/Jenkinsfile
