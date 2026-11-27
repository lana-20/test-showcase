#!/usr/bin/env python3
"""Write a suite's status JSON from its Allure results.

Every CI calls this through ci/run-<suite>.sh, so the page reads one format
no matter where a suite ran. Where it ran is detected from the CI's own
environment variables, never assumed:

  python3 ci/lib/status.py --suite playwright --results out/allure-results/playwright \
      --out out/status/playwright.json --exit-code 0
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import pathlib


def detect_ci() -> dict:
    env = os.environ
    host = env.get("HOST_CI_NAME")  # set when a CI runs inside another (Jenkins in GHA, kind in GHA)
    host_url = env.get("HOST_RUN_URL")
    if env.get("KUBERNETES_SERVICE_HOST"):
        return {"id": "kubernetes", "name": "Kubernetes (kind)" + (f" via {host}" if host else ""),
                "url": host_url}
    if env.get("JENKINS_HOME") or env.get("JENKINS_URL") or env.get("BUILD_TAG", "").startswith("jenkins-"):
        return {"id": "jenkins", "name": "Jenkins (Jenkinsfile Runner)" + (f" via {host}" if host else ""),
                "url": host_url or env.get("BUILD_URL")}
    if env.get("GITHUB_ACTIONS") == "true":
        return {"id": "github-actions", "name": "GitHub Actions",
                "url": f"{env['GITHUB_SERVER_URL']}/{env['GITHUB_REPOSITORY']}/actions/runs/{env['GITHUB_RUN_ID']}"}
    if env.get("GITLAB_CI") == "true":
        return {"id": "gitlab-ci", "name": "GitLab CI", "url": env.get("CI_JOB_URL")}
    if env.get("TF_BUILD") == "True":
        url = None
        if env.get("SYSTEM_COLLECTIONURI") and env.get("BUILD_BUILDID"):
            url = (f"{env['SYSTEM_COLLECTIONURI'].rstrip('/')}/{env.get('SYSTEM_TEAMPROJECT', '')}"
                   f"/_build/results?buildId={env['BUILD_BUILDID']}")
        return {"id": "azure-pipelines", "name": "Azure Pipelines", "url": url}
    if env.get("CIRCLECI") == "true":
        return {"id": "circleci", "name": "CircleCI", "url": env.get("CIRCLE_BUILD_URL")}
    if host:
        return {"id": "github-actions", "name": host, "url": host_url}
    return {"id": "local", "name": "Local machine", "url": None}


def count(results: pathlib.Path) -> dict:
    """Count the final outcome per test (latest attempt wins, as Allure shows it)."""
    latest: dict[str, dict] = {}
    for f in results.glob("*-result.json"):
        try:
            r = json.loads(f.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        key = r.get("historyId") or r.get("fullName") or r.get("uuid") or f.name
        if key not in latest or r.get("stop", 0) > latest[key].get("stop", 0):
            latest[key] = r
    totals = {"passed": 0, "failed": 0, "broken": 0, "skipped": 0, "unknown": 0}
    starts, stops = [], []
    for r in latest.values():
        status = r.get("status", "unknown")
        totals[status if status in totals else "unknown"] += 1
        if r.get("start"):
            starts.append(r["start"])
        if r.get("stop"):
            stops.append(r["stop"])
    totals["total"] = len(latest)
    totals["duration_ms"] = (max(stops) - min(starts)) if starts and stops else None
    return totals


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--suite", required=True)
    ap.add_argument("--results", required=True, type=pathlib.Path)
    ap.add_argument("--out", required=True, type=pathlib.Path)
    ap.add_argument("--exit-code", type=int, default=0)
    args = ap.parse_args()

    totals = count(args.results) if args.results.is_dir() else count(pathlib.Path("/nonexistent"))
    if totals["total"] == 0:
        outcome = "error"  # the suite produced no results at all: setup failed
    elif totals["failed"] or totals["broken"]:
        outcome = "failed"
    else:
        outcome = "passed"
    status = {
        "suite": args.suite,
        "outcome": outcome,
        "exit_code": args.exit_code,
        **totals,
        "finished_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        "ran_on": detect_ci(),
        "image": os.environ.get("SHOWCASE_IMAGE") or None,
        "commit": (os.environ.get("GITHUB_SHA") or os.environ.get("CI_COMMIT_SHA")
                   or os.environ.get("BUILD_SOURCEVERSION") or os.environ.get("CIRCLE_SHA1")
                   or os.environ.get("GIT_COMMIT") or None),
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(status, indent=2) + "\n", encoding="utf-8")
    print(f"[status] {args.suite}: {outcome} - {totals['passed']} passed, {totals['failed']} failed, "
          f"{totals['broken']} broken, {totals['skipped']} skipped (ran on {status['ran_on']['name']})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
