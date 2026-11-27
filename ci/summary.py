#!/usr/bin/env python3
"""Markdown table of every status JSON, for the GitHub Actions run summary."""
import json
import pathlib
import sys

rows = ["| Suite | Outcome | Passed | Failed | Skipped | Ran on |", "|---|---|---|---|---|---|"]
for f in sorted(pathlib.Path(sys.argv[1]).glob("*.json")):
    s = json.loads(f.read_text())
    rows.append(f"| {s['suite']} | {s['outcome']} | {s['passed']} | {s['failed'] + s.get('broken', 0)} "
                f"| {s['skipped']} | {s['ran_on']['name']} |")
print("### Test results\n\n" + "\n".join(rows))
