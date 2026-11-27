import { epic, feature } from "allure-js-commons";
import { DEMO_USER, expect, test } from "./fixtures";

test.describe("Sign in", () => {
  test.beforeEach(async () => {
    await epic("Tidepool web app");
    await feature("Sign in");
  });

  test("valid credentials land on the task board", async ({ page, login, board }) => {
    await login.open();
    await login.signInAs(DEMO_USER.username, DEMO_USER.password);
    await expect(page).toHaveURL(/board\.html$/);
    await expect(board.heading).toBeVisible();
    await expect(page.getByTestId("greeting")).toHaveText("Signed in as demo");
  });

  test("a wrong password shows an error and stays on the form", async ({ page, login }) => {
    await login.open();
    await login.signInAs(DEMO_USER.username, "not-the-password");
    await login.expectError("Wrong username or password.");
    await expect(page).toHaveURL(/index\.html$/);
  });

  test("empty fields are rejected before any request", async ({ login }) => {
    await login.open();
    await login.signIn.click();
    await login.expectError("Enter a username and a password.");
  });

  test("a protected page redirects to sign in, then back", async ({ page, login }) => {
    await page.goto("catalog.html");
    await expect(page).toHaveURL(/index\.html\?next=catalog\.html$/);
    await login.signInAs(DEMO_USER.username, DEMO_USER.password);
    await expect(page).toHaveURL(/catalog\.html$/);
  });
});
