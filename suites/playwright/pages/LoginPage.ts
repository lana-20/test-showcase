import { expect, type Locator, type Page } from "@playwright/test";
import { step } from "allure-js-commons";

export const DEMO_USER = { username: "demo", password: "tide-pass" };

export class LoginPage {
  readonly username: Locator;
  readonly password: Locator;
  readonly signIn: Locator;
  readonly error: Locator;

  constructor(private readonly page: Page) {
    this.username = page.getByLabel("Username");
    this.password = page.getByLabel("Password");
    this.signIn = page.getByRole("button", { name: "Sign in" });
    this.error = page.getByRole("alert");
  }

  async open(path = "index.html") {
    await step(`Open ${path}`, async () => {
      await this.page.goto(path);
    });
  }

  async signInAs(username: string, password: string) {
    await step(`Sign in as "${username}"`, async () => {
      await this.username.fill(username);
      await this.password.fill(password);
      await this.signIn.click();
    });
  }

  async expectError(message: string) {
    await step(`Error reads "${message}"`, async () => {
      await expect(this.error).toHaveText(message);
    });
  }
}
