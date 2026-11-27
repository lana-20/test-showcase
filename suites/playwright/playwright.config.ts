import { defineConfig, devices } from "@playwright/test";

// DEMO_URL points at an already running demo app (Docker Compose, Kubernetes).
// Without it, Playwright serves ../../demo-app itself on DEMO_PORT.
const PORT = Number(process.env.DEMO_PORT ?? 4173);
const DEMO_URL = process.env.DEMO_URL;
const baseURL = (DEMO_URL ?? `http://127.0.0.1:${PORT}`).replace(/\/?$/, "/");

export default defineConfig({
  testDir: "./tests",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: 0, // a flaky test should show up as flaky, not be hidden by retries
  workers: process.env.CI ? 2 : undefined,
  reporter: [
    ["list"],
    ["allure-playwright", {
      resultsDir: process.env.ALLURE_RESULTS_DIR ?? "allure-results",
      detail: true,
      suiteTitle: false,
      environmentInfo: { Suite: "Playwright", Browser: "Chromium", Node: process.version, Target: baseURL },
    }],
  ],
  use: {
    baseURL,
    testIdAttribute: "data-testid",
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: DEMO_URL ? undefined : {
    command: `node ../../demo-app/serve.mjs ${PORT}`,
    url: `${baseURL}index.html`,
    reuseExistingServer: !process.env.CI,
    stdout: "ignore",
    stderr: "pipe",
  },
});
