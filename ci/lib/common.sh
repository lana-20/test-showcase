# Sourced by ci/run-<suite>.sh. Sets REPO, OUT_DIR and RESULTS, and gives
# finish(), which writes the status JSON and passes the test exit code on.
set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT_DIR="${OUT_DIR:-$REPO/out}"
SUITE_DIR="$REPO/suites/$SUITE"
# A run can report under another name, e.g. playwright-k8s for the Kubernetes run.
RESULTS_NAME="${SHOWCASE_RESULTS_NAME:-$SUITE}"
RESULTS="$OUT_DIR/allure-results/$RESULTS_NAME"
STATUS_FILE="$OUT_DIR/status/$RESULTS_NAME.json"

mkdir -p "$RESULTS" "$OUT_DIR/status"
# Start clean so a report never mixes two runs.
find "$RESULTS" -mindepth 1 -delete

log() { printf '\n\033[1m[%s] %s\033[0m\n' "$SUITE" "$*"; }

finish() {
  local code=$1
  python3 "$REPO/ci/lib/status.py" --suite "$RESULTS_NAME" --results "$RESULTS" \
    --out "$STATUS_FILE" --exit-code "$code" || true
  exit "$code"
}
