#!/usr/bin/env bash
# Playwright (TypeScript) against the demo app.
# Uses DEMO_URL if set (Compose, Kubernetes); otherwise Playwright starts a local server.
SUITE=playwright
source "$(dirname "$0")/lib/common.sh"
cd "$SUITE_DIR"
[ -d node_modules ] || npm ci --no-audit --no-fund
log "Running Playwright"
set +e
ALLURE_RESULTS_DIR="$RESULTS" npx playwright test
code=$?
set -e
finish $code
