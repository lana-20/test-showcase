import { expect, type Locator, type Page } from "@playwright/test";
import { step } from "allure-js-commons";

export class CatalogPage {
  readonly search: Locator;
  readonly platform: Locator;
  readonly status: Locator;
  readonly results: Locator;
  readonly cartCount: Locator;
  readonly checkout: Locator;
  readonly confirmation: Locator;

  constructor(private readonly page: Page) {
    this.search = page.getByLabel("Search devices");
    this.platform = page.getByLabel("Platform");
    this.status = page.getByTestId("status");
    this.results = page.getByTestId("result");
    this.cartCount = page.getByTestId("cart-count");
    this.checkout = page.getByRole("button", { name: "Reserve devices" });
    this.confirmation = page.getByTestId("confirmation");
  }

  async open() {
    await step("Open the catalog and wait for devices to load", async () => {
      await this.page.goto("catalog.html");
      await expect(this.status).toHaveText(/\d+ devices?/);
    });
  }

  async searchFor(text: string) {
    await step(`Search for "${text}"`, async () => {
      await this.search.fill(text);
    });
  }

  async addToCart(name: string) {
    await step(`Add ${name} to the cart`, async () => {
      await this.page.getByRole("button", { name: `Add ${name} to cart` }).click();
    });
  }
}
