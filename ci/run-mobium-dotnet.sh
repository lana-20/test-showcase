#!/usr/bin/env bash
# The same Mobium scenarios in C#, under NUnit, xUnit and MSTest, driving
# ApiDemos on a booted Android emulator. The three runners take turns on the
# one device; their Allure results are merged into one report, labeled by runner.
SUITE=mobium-dotnet
source "$(dirname "$0")/lib/common.sh"
cd "$SUITE_DIR"
export APP_PATH="${APP_PATH:-$REPO/apps/ApiDemos-debug.apk}"
[ -f "$APP_PATH" ] || "$REPO/ci/android/fetch-apk.sh"
command -v mobium >/dev/null || "$REPO/ci/android/install-mobium.sh"
export DOTNET_CLI_TELEMETRY_OPTOUT=1 DOTNET_NOLOGO=1

log "Mobium .NET client source"
./fetch-client.sh

log "dotnet build"
dotnet build MobiumDotnet.sln -c Release

RAW="$OUT_DIR/raw/$SUITE"
TRX="$OUT_DIR/trx/$SUITE"
rm -rf "$RAW" "$TRX"
mkdir -p "$TRX"
code=0
run_tests() {   # run_tests <Runner label> <project suffix>
  local runner=$1 project=$2 dir="$RAW/$1"
  mkdir -p "$dir"
  printf '{"allure":{"directory":"%s"}}\n' "$dir" > "$RAW/$runner.allureConfig.json"
  log "dotnet test: $runner"
  set +e
  ALLURE_CONFIG="$RAW/$runner.allureConfig.json" dotnet test "tests/Showcase.Mobium.$project" \
    -c Release --no-build \
    --logger "trx;LogFileName=$runner.trx" --logger "console;verbosity=normal" \
    --results-directory "$TRX"
  local rc=$?
  set -e
  [ $rc -eq 0 ] || code=$rc
}
run_tests NUnit NUnit
run_tests xUnit Xunit
run_tests MSTest MSTest

log "Merge the three result sets into one report"
python3 merge_results.py --out "$RESULTS" --counts "$RAW/runners.json" \
  --environment "Runtime=.NET $(dotnet --version | cut -d. -f1-2)" \
  --environment "Runners=NUnit, xUnit, MSTest" \
  --environment "Mobium=${MOBIUM_VERSION:-v0.1.0-rc1}" \
  NUnit="$RAW/NUnit" xUnit="$RAW/xUnit" MSTest="$RAW/MSTest"

python3 "$REPO/ci/lib/status.py" --suite "$RESULTS_NAME" --results "$RESULTS" \
  --out "$STATUS_FILE" --exit-code "$code" || true
# Per-runner counts beside the totals, so the page and the summary can say
# how each runner did.
python3 - "$STATUS_FILE" "$RAW/runners.json" <<'PY' || true
import json, sys
status, runners = sys.argv[1], sys.argv[2]
s = json.load(open(status)); s["runners"] = json.load(open(runners))
open(status, "w").write(json.dumps(s, indent=2) + "\n")
PY
exit "$code"
