#!/usr/bin/env bash
# Selenium WebDriver BiDi (Java, JUnit 5) on Chrome, Firefox and Edge.
#   SELENIUM_REMOTE_URL  a Selenium Grid (CI: the hub Service on kind); unset = local browsers
#   DEMO_URL             the demo app as the browsers see it; unset = serve demo-app here on :8080
#   BROWSERS             default chrome,firefox,edge
SUITE=selenium-bidi
source "$(dirname "$0")/lib/common.sh"
if [ -z "${DEMO_URL:-}" ]; then
  node "$REPO/demo-app/serve.mjs" 8080 >/dev/null 2>&1 &
  server=$!
  trap 'kill $server 2>/dev/null || true' EXIT
  export DEMO_URL=http://127.0.0.1:8080/
  for _ in $(seq 1 50); do curl -fsS "$DEMO_URL" >/dev/null 2>&1 && break; sleep 0.2; done
fi
cd "$SUITE_DIR"
log "Running Selenium BiDi on ${BROWSERS:-chrome,firefox,edge} via ${SELENIUM_REMOTE_URL:-local browsers}, app at $DEMO_URL"
set +e
./mvnw -B -ntp test -Dallure.results.directory="$RESULTS"
code=$?
set -e
# Per-browser counts, from the Allure results (the browser is each result's parentSuite).
python3 - "$RESULTS" <<'PY' || true
import collections, json, pathlib, sys
counts = collections.defaultdict(collections.Counter)
for f in pathlib.Path(sys.argv[1]).glob("*-result.json"):
    r = json.loads(f.read_text())
    browser = next((l["value"] for l in r.get("labels", []) if l["name"] == "parentSuite"), "?")
    counts[browser][r.get("status", "unknown")] += 1
for browser, c in sorted(counts.items()):
    print(f"[selenium-bidi] {browser}: " + ", ".join(f"{n} {s}" for s, n in sorted(c.items())))
PY
finish $code
