#!/usr/bin/env python3
"""Nightly Robo crawl of ApiDemos on Firebase Test Lab: one real physical
device and one virtual device, turned into Allure results.

    python3 ci/firebase/robo.py --apk apps/ApiDemos-debug.apk

Needs an authenticated gcloud (see SETUP.md). Spark (no-cost) plan quota is
5 physical and 10 virtual runs a day; this uses one of each per night.

Note: Google has announced that Firebase Test Lab shuts down on 2027-09-30
(https://firebase.google.com/support/releases). See SETUP.md.
"""

from __future__ import annotations

import argparse
import json
import os
import pathlib
import re
import shutil
import subprocess
import sys
import tempfile
import time
import uuid

REPO = pathlib.Path(__file__).resolve().parents[2]
MIN_API = 26  # ApiDemos minSdkVersion


def gcloud(*args: str, check: bool = True) -> subprocess.CompletedProcess:
    print("+ gcloud " + " ".join(args), flush=True)
    return subprocess.run(["gcloud", *args], capture_output=True, text=True, check=check)


def best_version(model: dict) -> str | None:
    versions = [v for v in model.get("supportedVersionIds", []) if v.isdigit() and int(v) >= MIN_API]
    return max(versions, key=int) if versions else None


def pick_devices() -> tuple[dict, dict]:
    """A Google phone of each kind, newest Android it supports, avoiding
    models Test Lab tags as reduced-quota or deprecated."""
    models = json.loads(gcloud("firebase", "test", "android", "models", "list", "--format=json").stdout)

    def ok(m: dict, form: str) -> bool:
        tags = " ".join(m.get("tags", []) or []).lower()
        return (m.get("form") == form and m.get("formFactor") == "PHONE" and best_version(m)
                and "deprecated" not in tags and "reduced" not in tags)

    def rank(m: dict) -> tuple:
        return (m.get("brand", "").lower() == "google", int(best_version(m)))

    physical = sorted((m for m in models if ok(m, "PHYSICAL")), key=rank, reverse=True)
    virtual = sorted((m for m in models if ok(m, "VIRTUAL")), key=rank, reverse=True)
    if not physical or not virtual:
        raise SystemExit("no suitable physical/virtual phone found in the Test Lab catalog")
    return physical[0], virtual[0]


def allure_status(outcome: str) -> str:
    return {"passed": "passed", "failed": "failed", "skipped": "skipped"}.get(outcome.lower(), "broken")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--apk", default=str(REPO / "apps" / "ApiDemos-debug.apk"))
    ap.add_argument("--timeout", default="5m")
    args = ap.parse_args()

    out_dir = pathlib.Path(os.environ.get("OUT_DIR", REPO / "out"))
    results = out_dir / "allure-results" / "firebase-robo"
    shutil.rmtree(results, ignore_errors=True)
    results.mkdir(parents=True)

    physical, virtual = pick_devices()
    devices = [(physical, "real device"), (virtual, "virtual device")]
    for m, kind in devices:
        print(f"{kind}: {m.get('brand')} {m.get('name')} ({m['id']}), Android API {best_version(m)}")

    run_args = ["firebase", "test", "android", "run", "--type=robo", f"--app={args.apk}",
                f"--timeout={args.timeout}", "--results-history-name=test-showcase",
                "--client-details=matrixLabel=test-showcase", "--format=json"]
    for m, _ in devices:
        run_args.append(f"--device=model={m['id']},version={best_version(m)},locale=en,orientation=portrait")
    start = int(time.time() * 1000)
    run = gcloud(*run_args, check=False)
    stop = int(time.time() * 1000)
    sys.stderr.write(run.stderr)
    print(run.stdout)

    console = re.search(r"(https://console\.firebase\.google\.com/\S+?)\]?\s", run.stderr + " ")
    bucket = re.search(r"storage/browser/([^/\]\s]+)/([^\]\s]+?)/?\]", run.stderr)
    console_url = console.group(1).rstrip("].") if console else None
    try:
        outcomes = json.loads(run.stdout) if run.stdout.strip() else []
    except ValueError:
        outcomes = []

    tmp = pathlib.Path(tempfile.mkdtemp())
    if bucket:
        gs = f"gs://{bucket.group(1)}/{bucket.group(2)}"
        # Screenshots and the video only; skip the bulky logs.
        gcloud("storage", "cp", "-r", f"{gs}/*", str(tmp), check=False)

    for (m, kind), result in zip(devices, outcomes or [{}] * len(devices)):
        outcome = (result.get("outcome") or ("Error" if run.returncode not in (0, 10) else "Unknown"))
        title = f"Robo crawl on a {kind}: {m.get('brand')} {m.get('name')} (Android API {best_version(m)})"
        attachments = []
        folder = next((p for p in tmp.glob(f"{m['id']}-{best_version(m)}*") if p.is_dir()), None)
        if folder:
            shots = sorted(folder.rglob("*.png"))[:8]
            video = next(iter(folder.rglob("video.mp4")), None)
            files = shots + ([video] if video and video.stat().st_size < 25_000_000 else [])
            for f in files:
                src = f"{uuid.uuid4()}-attachment{f.suffix}"
                shutil.copy(f, results / src)
                attachments.append({"name": f.name, "source": src,
                                    "type": "video/mp4" if f.suffix == ".mp4" else "image/png"})
        res = {
            "uuid": str(uuid.uuid4()),
            "historyId": f"firebase-robo-{kind}",
            "name": title,
            "fullName": f"firebase.robo.{kind.replace(' ', '_')}",
            "status": allure_status(outcome),
            "statusDetails": {"message": f"Test Lab outcome: {outcome}. {result.get('test_details', '')}".strip()},
            "stage": "finished",
            "start": start,
            "stop": stop,
            "labels": [{"name": "epic", "value": "ApiDemos on Android"},
                       {"name": "feature", "value": "Firebase Test Lab Robo"},
                       {"name": "suite", "value": "Firebase Test Lab"},
                       {"name": "host", "value": m["id"]}],
            "links": [{"name": "Test Lab results", "url": console_url, "type": "link"}] if console_url else [],
            "attachments": attachments,
            "parameters": [{"name": "device", "value": f"{m['id']} / API {best_version(m)}"},
                           {"name": "kind", "value": kind}],
        }
        (results / f"{res['uuid']}-result.json").write_text(json.dumps(res), encoding="utf-8")

    env = dict(os.environ)
    env["RAN_ON_ID"] = "firebase-test-lab"
    env["RAN_ON_NAME"] = (f"a real {physical.get('brand')} {physical.get('name')} via Firebase Test Lab (Robo), "
                          f"from {os.environ.get('HOST_CI_NAME', 'GitHub Actions')}")
    if console_url:
        env["RAN_ON_URL"] = console_url
    subprocess.run([sys.executable, str(REPO / "ci" / "lib" / "status.py"), "--suite", "firebase-robo",
                    "--results", str(results), "--out", str(out_dir / "status" / "firebase-robo.json"),
                    "--exit-code", str(run.returncode)], env=env, check=True)
    return 0 if run.returncode in (0, 10) else run.returncode


if __name__ == "__main__":
    raise SystemExit(main())
