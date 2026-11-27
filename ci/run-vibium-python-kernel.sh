#!/usr/bin/env bash
# Vibium's Python client against a Kernel cloud browser, over WebDriver BiDi.
# Vibium speaks W3C protocols only, so it uses Kernel's webdriver_ws_url
# (not the CDP URL) through VIBIUM_CONNECT_URL.
SUITE=vibium-python
export SHOWCASE_RESULTS_NAME=vibium-python-kernel
source "$(dirname "$0")/lib/common.sh"
: "${KERNEL_API_KEY:?KERNEL_API_KEY is not set}"
export DEMO_URL="${DEMO_URL:-https://lana-20.github.io/test-showcase/demo/}"
export RAN_ON_ID=kernel RAN_ON_NAME="Kernel cloud browser via ${HOST_CI_NAME:-GitHub Actions}"
source "$REPO/ci/lib/kernel.sh"
kernel_create
cd "$SUITE_DIR"
unset VIBIUM_BIN_PATH
PY="${PYTHON:-python3}"
log "Running Vibium Python on Kernel session $KERNEL_SESSION_ID"
set +e
VIBIUM_CONNECT_URL="$KERNEL_BIDI_URL" timeout 600 "$PY" -m pytest --alluredir="$RESULTS" --clean-alluredir
code=$?
set -e
finish $code
