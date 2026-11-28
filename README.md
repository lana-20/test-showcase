# test-showcase

**Live site: https://lana-20.github.io/test-showcase** · [All reports](https://lana-20.github.io/test-showcase/reports/)

Seven small, real test suites. Each one has its own CI, runs from one script, and publishes an Allure report with history to GitHub Pages.

| # | Suite | Stack | Target | CI |
|---|---|---|---|---|
| 1 | Playwright | TypeScript, `@playwright/test`, allure-playwright | Demo web app (Chromium) | GitHub Actions, plus a Kubernetes Job on kind |
| 2 | Vibium · Python client | Python, pytest, `vibium`, allure-pytest | Demo web app (Chrome over WebDriver BiDi) | GitLab CI |
| 3 | Appium · Java | Java 21, JUnit 6, Appium java-client, allure-jupiter | ApiDemos on an Android 15 emulator | Jenkins (declarative Jenkinsfile in Jenkinsfile Runner) |
| 4 | Vibium · MCP server | Python, MCP SDK, pytest-asyncio, allure-pytest | `vibium mcp` over stdio, driving the demo app | Azure Pipelines |
| 5 | Mobium | Python, Mobium client, pytest, allure-pytest | ApiDemos on an Android 15 emulator | CircleCI |
| 6 | Selenium WebDriver BiDi | Java 21, Selenium 4.54, JUnit 5, allure-junit5 | Demo web app on Chrome, Firefox and Edge (WebDriver BiDi) | Selenium Grid on Kubernetes (kind), from GitHub Actions |
| 7 | Mobium · C# (.NET) | C#, .NET 10, Mobium .NET client, NUnit, xUnit and MSTest, Allure | ApiDemos on an Android 15 emulator | GitHub Actions (all three runners in one job) |

Until an external CI account is connected, that suite runs on GitHub Actions from the same script and image. The page says so ("Ran on: GitHub Actions · GitLab CI pipeline ready") and never shows a CI that did not produce the report. [SETUP.md](SETUP.md) covers connecting each account.

Extra legs:

- **Kubernetes (kind):** the Playwright image runs as a Job against the demo app's in-cluster Service on every run ([k8s/](k8s/)).
  The Selenium BiDi suite runs the same way, as a Job, against a Selenium Grid (hub plus Chrome, Firefox and Edge
  nodes) deployed in its own kind cluster.
- **Kernel cloud browsers** (nightly): Playwright over CDP and Vibium over WebDriver BiDi, against the public copy of the demo app at `/demo/`.
- **Firebase Test Lab** (nightly): a Robo crawl of ApiDemos on one real physical phone and one virtual device. Google is shutting Test Lab down on 2027-09-30; see SETUP.md.

The Kernel and Firebase legs show "ready, pending account" until their keys exist. Free open-source programs from
device clouds are written up, but not applied for, in [docs/cloud-oss-programs.md](docs/cloud-oss-programs.md).

## How it fits together

```
suites/<suite>/          the tests (page/screen objects, fixtures, Allure steps)
demo-app/                Tidepool: a tiny web app the web suites test (served locally; /lab needs Basic auth)
ci/run-<suite>.sh        the one entry point every CI calls
ci/lib/status.py         writes out/status/<suite>.json (counts, time, where it ran, run URL)
docker/                  one image per container-friendly suite, published to GHCR
Jenkinsfile              Appium Java pipeline (runs in ci/jenkins/Dockerfile)
.gitlab-ci.yml           Vibium Python on GitLab CI
azure-pipelines.yml      Vibium MCP on Azure Pipelines
.circleci/config.yml     Mobium on CircleCI's Android machine image
k8s/                     kind cluster config, demo app, Selenium Grid, and the Playwright and Selenium BiDi Jobs
ci/fetch_external.py     downloads results from GitLab, Azure DevOps and CircleCI
ci/build_site.py         generates one Allure report per suite (with history) and the site
site/                    the page, the banner and the reports index
docs/                    notes, e.g. when to choose WebDriver BiDi, CDP or Playwright
```

Every run writes `out/allure-results/<suite>/` and `out/status/<suite>.json`. The GitHub Actions workflow
([.github/workflows/showcase.yml](.github/workflows/showcase.yml)) gathers them from every CI. It restores
each report's `history/` from the live site, so trends carry across runs, then generates the reports and
deploys to Pages. A red suite still gets its report, and it never blocks the deploy.

The workflow runs on every push to `main`, nightly at 06:00 UTC, and on demand (**Actions → Showcase → Run workflow**).

## Run a suite locally

Every suite runs from the repo root with `ci/run-<suite>.sh`. Results go to `out/`.

### With Docker Compose (web suites, no local toolchain)

```sh
mkdir -p out && chmod 777 out          # the containers run as non-root users
docker compose up -d demo-app
docker compose run --rm playwright      # or vibium-python, vibium-mcp
docker compose down
```

This pulls `ghcr.io/lana-20/test-showcase-<suite>:latest`, the same images CI uses. To build them from your
checkout, add `--build`.

### Playwright

```sh
cd suites/playwright && npm ci && npx playwright install chromium && cd ../..
ci/run-playwright.sh                    # serves demo-app itself; set DEMO_URL to use another server
```

### Vibium (Python client) and Vibium MCP

```sh
python3 -m venv .venv && . .venv/bin/activate
pip install -r suites/vibium-python/requirements.txt -r suites/vibium-mcp/requirements.txt
vibium install                          # downloads Chrome for Testing
ci/run-vibium-python.sh
ci/run-vibium-mcp.sh
```

`HEADED=1` shows the browser. The scripts unset `VIBIUM_BIN_PATH` so the pinned binary from the wheel is
used, not a local development build.

### Appium Java

Needs JDK 21, an Android emulator or device (`adb devices`), and Appium:

```sh
npm install -g appium@4.0.1 && appium driver install uiautomator2@10.4.0
appium &                                # http://127.0.0.1:4723
ci/run-appium-java.sh                   # downloads ApiDemos v6.0.18 and checks its SHA-256
```

To run it as CI does, through Jenkins: `JENKINS_IMAGE=ghcr.io/lana-20/test-showcase-jenkins-runner:latest ci/jenkins/run-on-host.sh`.

### Mobium

Needs an Android emulator or device and Python 3.9+:

```sh
ci/android/install-mobium.sh            # pinned v0.1.0-rc1 release, checksum-verified
pip install -r suites/mobium/requirements.txt
ci/run-mobium.sh
```

### Mobium · C# (.NET)

The same scenarios under NUnit, xUnit and MSTest, one runner after the other. Needs the .NET 10 SDK, an
Android emulator or device, and Mobium (see above). Runner comparison: [suites/mobium-dotnet](suites/mobium-dotnet/README.md).

```sh
ci/run-mobium-dotnet.sh                 # fetches the Mobium .NET client at the pinned tag, builds, runs all three
```

### Selenium WebDriver BiDi

Needs JDK 21. Without a Grid it starts Chrome, Firefox and Edge on your machine (Selenium Manager fetches the
drivers, and Firefox if it is missing) and serves the demo app itself:

```sh
ci/run-selenium-bidi.sh                       # all three browsers, headless
BROWSERS=chrome,firefox HEADLESS=0 ci/run-selenium-bidi.sh
```

Against a Selenium Grid in Docker, as in CI (Edge images are amd64 only; on Apple silicon leave out `edge` and
set `BROWSERS=chrome,firefox`):

```sh
mkdir -p out && chmod 777 out
docker compose up -d demo-app selenium-hub chrome firefox edge
docker compose run --rm selenium-bidi
docker compose down
```

On kind, exactly as CI runs it (Grid manifests in [k8s/selenium-grid.yaml](k8s/selenium-grid.yaml)):

```sh
mkdir -p /tmp/k8s-out && chmod 777 /tmp/k8s-out
kind create cluster --name showcase --config k8s/kind-config.yaml
for img in demo-app selenium-bidi; do
  docker pull ghcr.io/lana-20/test-showcase-$img:latest
  docker tag ghcr.io/lana-20/test-showcase-$img:latest ghcr.io/lana-20/test-showcase-$img:ci
done
ci/k8s-selenium-bidi.sh                 # results in /tmp/k8s-out
```

When to pick BiDi over CDP or Playwright: [docs/bidi-cdp-playwright.md](docs/bidi-cdp-playwright.md).

### Playwright on Kubernetes (kind)

```sh
mkdir -p /tmp/k8s-out && chmod 777 /tmp/k8s-out
kind create cluster --name showcase --config k8s/kind-config.yaml
docker pull ghcr.io/lana-20/test-showcase-demo-app:latest && docker tag ghcr.io/lana-20/test-showcase-demo-app:latest ghcr.io/lana-20/test-showcase-demo-app:ci
docker pull ghcr.io/lana-20/test-showcase-playwright:latest && docker tag ghcr.io/lana-20/test-showcase-playwright:latest ghcr.io/lana-20/test-showcase-playwright:ci
ci/k8s-run.sh                           # results in /tmp/k8s-out
```

### View a report

```sh
npx allure-commandline@2.46.2 serve out/allure-results/playwright
```

## Good practice the suites show

- Page or screen objects own every locator: roles, labels, test ids and resource ids. No brittle XPath.
- No sleeps. Playwright uses web-first assertions. Vibium uses its own actionability waits plus `wait_for_function`. Appium uses explicit `WebDriverWait`s. Mobium uses `wait_for`. Selenium BiDi waits on the browser's own events (log, network, browsing context) and `WebDriverWait`. The demo app adds random delays on purpose.
- A fresh state for every test: a new browser context, a fresh app launch, or a direct activity start.
- Allure steps on every action, plus a screenshot (and page source or screen map) on failure.
- Pinned versions everywhere: npm and pip packages, Maven artifacts, Docker base images (the Jenkins runner by digest), GitHub Actions, the emulator image, and the ApiDemos and Mobium downloads (both checksum-verified).
- No retries, so a flaky test shows up as flaky.

## License

[MIT](LICENSE)
