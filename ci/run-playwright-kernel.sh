#!/usr/bin/env bash
# Playwright against a Kernel cloud browser (kernel.sh), over CDP.
# The browser is in Kernel's cloud, so it tests the public copy of the demo
# app on GitHub Pages. Kept tiny on purpose: one browser, one worker, a hard
# timeout, and the session is deleted on exit whatever happens.
SUITE=playwright
export SHOWCASE_RESULTS_NAME=playwright-kernel
source "$(dirname "$0")/lib/common.sh"
: "${KERNEL_API_KEY:?KERNEL_API_KEY is not set}"
export DEMO_URL="${DEMO_URL:-https://lana-20.github.io/test-showcase/demo/}"
export RAN_ON_ID=kernel RAN_ON_NAME="Kernel cloud browser via ${HOST_CI_NAME:-GitHub Actions}"
source "$REPO/ci/lib/kernel.sh"
kernel_create
cd "$SUITE_DIR"
[ -d node_modules ] || npm ci --no-audit --no-fund
log "Running Playwright on Kernel session $KERNEL_SESSION_ID"
set +e
KERNEL_CDP_URL="$KERNEL_CDP_URL" ALLURE_RESULTS_DIR="$RESULTS" timeout 600 npx playwright test --workers=1
code=$?
set -e
finish $code
