import { test as base, expect } from "@playwright/test";
import { BoardPage } from "../pages/BoardPage";
import { CatalogPage } from "../pages/CatalogPage";
import { DEMO_USER, LoginPage } from "../pages/LoginPage";

type Pages = { login: LoginPage; board: BoardPage; catalog: CatalogPage; signedIn: void };

export const test = base.extend<Pages>({
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
