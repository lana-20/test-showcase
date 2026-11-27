import { test as base, chromium, expect, type Browser } from "@playwright/test";
import { BoardPage } from "../pages/BoardPage";
import { CatalogPage } from "../pages/CatalogPage";
import { DEMO_USER, LoginPage } from "../pages/LoginPage";

type Pages = { login: LoginPage; board: BoardPage; catalog: CatalogPage; signedIn: void };

// With KERNEL_CDP_URL set (ci/run-playwright-kernel.sh), tests run in a Kernel
// cloud browser over CDP instead of a local Chromium. Each test still gets a
// fresh context. The session itself is deleted by the script, not here.
const kernelCdpUrl = process.env.KERNEL_CDP_URL;

export const test = base.extend<Pages, { browser: Browser }>({
  ...(kernelCdpUrl
    ? {
        browser: [
          async ({}, use) => {
            const browser = await chromium.connectOverCDP(kernelCdpUrl, { timeout: 30_000 });
            await use(browser);
            await browser.close(); // disconnects; the cloud session is deleted by the run script
          },
          { scope: "worker" },
        ],
      }
    : {}),
  login: async ({ page }, use) => use(new LoginPage(page)),
  board: async ({ page }, use) => use(new BoardPage(page)),
  catalog: async ({ page }, use) => use(new CatalogPage(page)),
  // Signs in through the UI once per test; each test gets a fresh context.
  signedIn: async ({ login, board }, use) => {
    await login.open();
    await login.signInAs(DEMO_USER.username, DEMO_USER.password);
    await expect(board.heading).toBeVisible();
    await use();
  },
});

export { expect, DEMO_USER };
