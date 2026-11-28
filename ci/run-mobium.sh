#!/usr/bin/env bash
# Mobium (Python client) driving ApiDemos on a booted Android emulator.
SUITE=mobium
source "$(dirname "$0")/lib/common.sh"
cd "$SUITE_DIR"
export APP_PATH="${APP_PATH:-$REPO/apps/ApiDemos-debug.apk}"
[ -f "$APP_PATH" ] || "$REPO/ci/android/fetch-apk.sh"
command -v mobium >/dev/null || "$REPO/ci/android/install-mobium.sh"
"$REPO/ci/android/quiet-system-dialogs.sh"   # no launcher ANR dialog over the app
PY="${PYTHON:-python3}"
"$PY" -c "import mobium" 2>/dev/null || "$PY" -m pip install -q -r requirements.txt
log "mobium doctor"
mobium doctor || true
log "Running Mobium suite"
set +e
"$PY" -m pytest --alluredir="$RESULTS" --clean-alluredir
code=$?
set -e
finish $code
