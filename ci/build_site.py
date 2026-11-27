#!/usr/bin/env python3
"""Build the Pages site: the static page, one Allure report per suite, and
the status JSON the page reads.

    python3 ci/build_site.py --collected collected --out _site \
        --live https://lana-20.github.io/test-showcase

`collected/` holds what each CI produced, wherever it ran:
    collected/allure-results/<id>/   collected/status/<id>.json

History: Allure trends need the previous report's history/ folder. It is
fetched from the live site before generating, so no state lives in the repo.
"""

from __future__ import annotations

import argparse
import datetime as dt
import html
import json
import os
import pathlib
import shutil
import subprocess
import urllib.error
import urllib.request

REPO = pathlib.Path(__file__).resolve().parents[1]
HISTORY_FILES = ["history.json", "history-trend.json", "duration-trend.json",
                 "categories-trend.json", "retry-trend.json"]


def report_ids(config: dict) -> list[tuple[str, str, bool]]:
    """(id, title, optional). Optional runs (cloud legs that need an account)
    are left out entirely until they have produced a result."""
    ids = []
    for s in config["suites"]:
        ids.append((s["id"], s["title"], False))
        for x in s.get("extra", []):
            ids.append((x["id"], x["label"], bool(x.get("optional"))))
    return ids


def fetch(url: str) -> bytes | None:
    try:
        with urllib.request.urlopen(url, timeout=20) as r:
            return r.read()
    except (urllib.error.URLError, TimeoutError, OSError):
        return None


def restore_history(live: str, rid: str, results: pathlib.Path) -> int:
    hist = results / "history"
    hist.mkdir(exist_ok=True)
    got = 0
    for name in HISTORY_FILES:
        body = fetch(f"{live}/reports/{rid}/history/{name}")
        if body and body.strip().startswith((b"[", b"{")):
            (hist / name).write_bytes(body)
            got += 1
    return got


def write_executor(results: pathlib.Path, status: dict | None, live: str, rid: str) -> None:
    ran = (status or {}).get("ran_on") or {}
    run_id = os.environ.get("GITHUB_RUN_ID", "local")
    executor = {
        "name": ran.get("name", "Unknown"),
        "type": ran.get("id", "unknown"),
        "buildName": f"{ran.get('name', 'run')} #{os.environ.get('GITHUB_RUN_NUMBER', run_id)}",
        "buildOrder": int(os.environ.get("GITHUB_RUN_NUMBER", "0") or 0),
        "buildUrl": ran.get("url") or "",
        "reportUrl": f"{live}/reports/{rid}/",
        "reportName": rid,
    }
    (results / "executor.json").write_text(json.dumps(executor), encoding="utf-8")
    env_lines = [f"Ran.on={ran.get('name', 'unknown')}"]
    if status and status.get("image"):
        env_lines.append(f"Image={status['image']}")
    if status and status.get("commit"):
        env_lines.append(f"Commit={status['commit'][:12]}")
    existing = results / "environment.properties"
    prior = existing.read_text(encoding="utf-8") if existing.exists() else ""
    existing.write_text(prior + ("\n" if prior and not prior.endswith("\n") else "") + "\n".join(env_lines) + "\n",
                        encoding="utf-8")


BACK_BAR = """
<style>
  #showcase-bar{{position:fixed;right:14px;bottom:14px;z-index:9999;display:flex;gap:10px;align-items:center;
    font:600 13px/1.2 system-ui,sans-serif;background:#10324f;color:#fff;padding:9px 14px;border-radius:999px;
    box-shadow:0 2px 10px rgba(0,0,0,.25)}}
  #showcase-bar a{{color:#fff;text-decoration:underline;text-underline-offset:2px}}
</style>
<div id="showcase-bar" role="navigation" aria-label="Showcase">
  <span>{title} suite</span><a href="../">All reports</a><a href="../../#{sid}">Showcase</a>
</div>
"""


def add_back_bar(index: pathlib.Path, title: str, sid: str) -> None:
    text = index.read_text(encoding="utf-8")
    bar = BACK_BAR.format(title=html.escape(title), sid=html.escape(sid))
    text = text.replace("</body>", bar + "</body>", 1)
    index.write_text(text, encoding="utf-8")


def placeholder(report_dir: pathlib.Path, title: str, reason: str) -> None:
    report_dir.mkdir(parents=True, exist_ok=True)
    (report_dir / "index.html").write_text(f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)} report</title><link rel="stylesheet" href="../../styles.css"></head>
<body><main class="wrap" style="padding:48px 16px"><p><a href="../">← All reports</a></p>
<h1>{html.escape(title)}</h1><p>{html.escape(reason)}</p></main></body></html>
""", encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--collected", type=pathlib.Path, required=True)
    ap.add_argument("--out", type=pathlib.Path, required=True)
    ap.add_argument("--live", default="https://lana-20.github.io/test-showcase")
    ap.add_argument("--allure", default="allure")
    args = ap.parse_args()

    if args.out.exists():
        shutil.rmtree(args.out)
    shutil.copytree(REPO / "site", args.out)
    config = json.loads((REPO / "site" / "suites.json").read_text(encoding="utf-8"))
    status_out = args.out / "data" / "status"
    status_out.mkdir(parents=True, exist_ok=True)

    summary = []
    parent_of = {x["id"]: s["id"] for s in config["suites"] for x in s.get("extra", [])}
    # A public copy of the demo app, for cloud browsers that cannot reach a CI runner.
    shutil.copytree(REPO / "demo-app", args.out / "demo")

    for rid, title, optional in report_ids(config):
        results = args.collected / "allure-results" / rid
        status_file = args.collected / "status" / f"{rid}.json"
        status = json.loads(status_file.read_text(encoding="utf-8")) if status_file.exists() else None
        report_dir = args.out / "reports" / rid
        has_results = results.is_dir() and any(results.glob("*-result.json"))

        if status is None and not has_results and optional:
            summary.append(f"{rid}: not run (optional, pending account)")
            continue
        if status is None:
            # The job never reached the run script (for example, the emulator did not boot).
            status = {
                "suite": rid, "outcome": "error", "exit_code": None, "passed": 0, "failed": 0, "broken": 0,
                "skipped": 0, "unknown": 0, "total": 0, "duration_ms": None,
                "finished_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
                "ran_on": {"id": "github-actions", "name": "GitHub Actions",
                           "url": os.environ.get("RUN_URL")},
                "image": None, "commit": os.environ.get("GITHUB_SHA"),
                "note": "The suite job ended before the tests ran; see the CI run log.",
            }
        (status_out / f"{rid}.json").write_text(json.dumps(status, indent=2) + "\n", encoding="utf-8")

        if has_results:
            restored = restore_history(args.live, rid, results)
            write_executor(results, status, args.live, rid)
            subprocess.run([args.allure, "generate", str(results), "-o", str(report_dir), "--clean",
                            "--name", f"{title} · test-showcase"], check=True)
            add_back_bar(report_dir / "index.html", title, parent_of.get(rid, rid))
            summary.append(f"{rid}: report generated ({restored} history files restored)")
        else:
            placeholder(report_dir, title, status.get("note") or "This run produced no test results. The CI log has the details.")
            summary.append(f"{rid}: no results, placeholder written")

    (args.out / ".nojekyll").write_text("", encoding="utf-8")
    print("\n".join(summary))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
