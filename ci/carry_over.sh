#!/usr/bin/env bash
# Optional cloud legs (Kernel, Firebase Test Lab) run nightly only. On other
# runs, reuse their most recent results artifact so the page keeps showing
# the last real run (with its original timestamp) instead of dropping it.
#   ci/carry_over.sh collected playwright-kernel vibium-python-kernel firebase-robo
set -euo pipefail
INTO="$1"; shift
for id in "$@"; do
  if [ -f "$INTO/status/$id.json" ]; then echo "$id: ran in this workflow"; continue; fi
  run_id="$(gh api "repos/$GITHUB_REPOSITORY/actions/artifacts?name=results-$id&per_page=1" \
    --jq '.artifacts[0] | select(.expired == false) | .workflow_run.id' 2>/dev/null || true)"
  if [ -z "$run_id" ]; then echo "$id: no earlier run (pending account)"; continue; fi
  tmp="$(mktemp -d)"
  if gh run download "$run_id" -R "$GITHUB_REPOSITORY" -n "results-$id" -D "$tmp"; then
    mkdir -p "$INTO/allure-results" "$INTO/status"
    cp -r "$tmp/allure-results/$id" "$INTO/allure-results/" 2>/dev/null || true
    cp "$tmp/status/$id.json" "$INTO/status/" 2>/dev/null || true
    # Mark it, so the report is rebuilt without adding a second point to the trend.
    touch "$INTO/allure-results/$id/.carried-over" 2>/dev/null || true
    echo "$id: carried over from run $run_id"
  fi
done
