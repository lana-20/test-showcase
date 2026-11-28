#!/usr/bin/env python3
"""Merge the three runners' Allure results into one result set.

Each runner writes its own allure-results directory. This copies them into
one, and labels every test with the runner that ran it (parentSuite, the
framework label and a tag), so the report's Suites tab shows the same tests three times:
under NUnit, under xUnit and under MSTest. It also writes per-runner counts
for the status JSON.

    python3 merge_results.py --out out/allure-results/mobium-dotnet \
        --counts out/status/mobium-dotnet.runners.json \
        NUnit=raw/nunit xUnit=raw/xunit MSTest=raw/mstest
"""

from __future__ import annotations

import argparse
import json
import pathlib
import shutil

REPLACED = {"parentSuite", "suite", "subSuite", "framework"}


def merge(runner: str, src: pathlib.Path, out: pathlib.Path) -> dict:
    counts = {"passed": 0, "failed": 0, "broken": 0, "skipped": 0, "unknown": 0, "total": 0}
    if not src.is_dir():
        return counts
    for f in src.iterdir():
        if f.is_dir() or f.name in ("environment.properties", "executor.json"):
            continue
        if f.name.endswith("-result.json"):
            r = json.loads(f.read_text(encoding="utf-8"))
            old = r.get("labels", [])
            # One tree for all three: runner, then test class, then the test.
            test_class = next((l["value"] for l in old if l.get("name") == "testClass"), "")
            test_class = test_class.rsplit(".", 1)[-1] or next(
                (l["value"] for l in old if l.get("name") in ("subSuite", "suite")), "")
            labels = [l for l in old if l.get("name") not in REPLACED]
            labels += [{"name": "parentSuite", "value": runner},
                       {"name": "suite", "value": test_class},
                       {"name": "framework", "value": runner},
                       {"name": "tag", "value": runner}]
            r["labels"] = labels
            # Keep the runner in the identity, so the same test under two runners
            # is two tests in the report and in its history.
            for key in ("historyId", "testCaseId"):
                if r.get(key):
                    r[key] = f"{runner}:{r[key]}"
            (out / f.name).write_text(json.dumps(r), encoding="utf-8")
            status = r.get("status", "unknown")
            counts[status if status in counts else "unknown"] += 1
            counts["total"] += 1
        else:
            shutil.copy(f, out / f.name)
    return counts


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True, type=pathlib.Path)
    ap.add_argument("--counts", required=True, type=pathlib.Path)
    ap.add_argument("--environment", action="append", default=[], help="KEY=VALUE for environment.properties")
    ap.add_argument("runners", nargs="+", help="Runner=results-dir")
    args = ap.parse_args()

    args.out.mkdir(parents=True, exist_ok=True)
    per_runner = {}
    for spec in args.runners:
        runner, _, path = spec.partition("=")
        per_runner[runner] = merge(runner, pathlib.Path(path), args.out)
        c = per_runner[runner]
        print(f"[merge] {runner}: {c['passed']} passed, {c['failed']} failed, {c['broken']} broken, "
              f"{c['skipped']} skipped of {c['total']}")
    (args.out / "environment.properties").write_text("\n".join(args.environment) + "\n", encoding="utf-8")
    args.counts.parent.mkdir(parents=True, exist_ok=True)
    args.counts.write_text(json.dumps(per_runner, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
