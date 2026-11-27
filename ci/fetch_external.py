#!/usr/bin/env python3
"""Download the latest results a suite produced on its own CI.

Runs in the GitHub Actions report job. Each source is used only when its
repository variable says that suite has moved there, and only reads (all
tokens are read-only and come from GitHub secrets):

  VIBIUM_PYTHON_CI=gitlab-ci       GitLab API, job "vibium-python" on main
  VIBIUM_MCP_CI=azure-pipelines    Azure DevOps REST API, artifact "vibium-mcp"
  MOBIUM_CI=circleci               CircleCI API v2, job "mobium" artifacts

Each artifact holds out/allure-results/<suite>/ and out/status/<suite>.json,
which are copied into --into. The latest *finished* run is used, passed or
failed, because a failing run is a result the page should show.
A source that fails to download is reported and skipped; the report job
then shows that suite as having no results rather than stale ones.
"""

from __future__ import annotations

import argparse
import base64
import io
import json
import os
import pathlib
import shutil
import sys
import urllib.error
import urllib.parse
import urllib.request
import zipfile

UA = {"User-Agent": "test-showcase-report"}


def get(url: str, headers: dict | None = None) -> bytes:
    req = urllib.request.Request(url, headers={**UA, **(headers or {})})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def get_json(url: str, headers: dict | None = None):
    return json.loads(get(url, headers))


def unpack_out(zip_bytes: bytes, into: pathlib.Path, suite: str) -> None:
    """Copy allure-results/<suite> and status/<suite>.json from an artifact zip."""
    with zipfile.ZipFile(io.BytesIO(zip_bytes)) as z:
        for name in z.namelist():
            if name.endswith("/"):
                continue
            parts = pathlib.PurePosixPath(name).parts
            for anchor in ("allure-results", "status"):
                if anchor in parts:
                    rel = pathlib.Path(*parts[parts.index(anchor):])
                    if suite not in str(rel):
                        continue
                    dest = into / rel
                    dest.parent.mkdir(parents=True, exist_ok=True)
                    dest.write_bytes(z.read(name))
                    break


def gitlab(into: pathlib.Path) -> None:
    project = os.environ["GITLAB_PROJECT"]
    token = os.environ.get("GITLAB_READ_TOKEN", "")
    base = f"https://gitlab.com/api/v4/projects/{urllib.parse.quote(project, safe='')}"
    h = {"PRIVATE-TOKEN": token} if token else {}
    jobs = get_json(f"{base}/jobs?scope[]=success&scope[]=failed&per_page=50", h)
    job = next(j for j in jobs if j["name"] == "vibium-python" and j["ref"] == "main")
    print(f"GitLab: job {job['id']} ({job['status']}) {job['web_url']}")
    unpack_out(get(f"{base}/jobs/{job['id']}/artifacts", h), into, "vibium-python")


def azure(into: pathlib.Path) -> None:
    org, project = os.environ["AZURE_DEVOPS_ORG"], os.environ["AZURE_DEVOPS_PROJECT"]
    pat = os.environ["AZURE_DEVOPS_TOKEN"]
    h = {"Authorization": "Basic " + base64.b64encode(f":{pat}".encode()).decode()}
    base = f"https://dev.azure.com/{org}/{urllib.parse.quote(project)}/_apis/build"
    builds = get_json(f"{base}/builds?branchName=refs/heads/main&statusFilter=completed"
                      f"&queryOrder=finishTimeDescending&$top=20&api-version=7.1", h)["value"]
    for b in builds:
        try:
            art = get_json(f"{base}/builds/{b['id']}/artifacts?artifactName=vibium-mcp&api-version=7.1", h)
        except urllib.error.HTTPError:
            continue
        print(f"Azure Pipelines: build {b['id']} ({b.get('result')}) {b['_links']['web']['href']}")
        unpack_out(get(art["resource"]["downloadUrl"], h), into, "vibium-mcp")
        return
    raise RuntimeError("no completed Azure Pipelines build on main has a vibium-mcp artifact")


def circleci(into: pathlib.Path) -> None:
    h = {"Circle-Token": os.environ["CIRCLECI_TOKEN"]}
    slug = os.environ.get("CIRCLECI_SLUG", "gh/lana-20/test-showcase")
    api = "https://circleci.com/api/v2"
    pipelines = get_json(f"{api}/project/{slug}/pipeline?branch=main", h)["items"]
    for p in pipelines:
        for wf in get_json(f"{api}/pipeline/{p['id']}/workflow", h)["items"]:
            if wf["status"] not in ("success", "failed"):
                continue
            for job in get_json(f"{api}/workflow/{wf['id']}/job", h)["items"]:
                if job["name"] != "mobium" or not job.get("job_number"):
                    continue
                arts = get_json(f"{api}/project/{slug}/{job['job_number']}/artifacts", h)["items"]
                if not arts:
                    continue
                print(f"CircleCI: job {job['job_number']} ({wf['status']})")
                for a in arts:
                    path = pathlib.PurePosixPath(a["path"])
                    parts = path.parts[1:] if path.parts and path.parts[0] == "out" else path.parts
                    if not parts or parts[0] not in ("allure-results", "status"):
                        continue
                    dest = into / pathlib.Path(*parts)
                    dest.parent.mkdir(parents=True, exist_ok=True)
                    dest.write_bytes(get(a["url"], h))
                return
    raise RuntimeError("no finished CircleCI mobium job with artifacts on main")


SOURCES = [
    ("VIBIUM_PYTHON_CI", "gitlab-ci", "vibium-python", gitlab),
    ("VIBIUM_MCP_CI", "azure-pipelines", "vibium-mcp", azure),
    ("MOBIUM_CI", "circleci", "mobium", circleci),
]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--into", type=pathlib.Path, required=True)
    args = ap.parse_args()
    for var, value, suite, fn in SOURCES:
        if os.environ.get(var) != value:
            print(f"{suite}: runs on GitHub Actions ({var} is not '{value}')")
            continue
        # Never mix a GitHub Actions run with the external one.
        shutil.rmtree(args.into / "allure-results" / suite, ignore_errors=True)
        (args.into / "status" / f"{suite}.json").unlink(missing_ok=True)
        try:
            fn(args.into)
        except Exception as exc:  # report and continue: one CI must not block the site
            print(f"::warning::{suite}: could not fetch results from {value}: {exc}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
