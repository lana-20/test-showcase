import { epic, feature } from "allure-js-commons";
import { expect, test } from "./fixtures";

test.describe("Device catalog", () => {
  test.beforeEach(async ({ signedIn, catalog }) => {
    void signedIn;
    await epic("Tidepool web app");
    await feature("Catalog");
    await catalog.open();
  });

  test("devices load asynchronously and can be searched", async ({ catalog }) => {
    await expect(catalog.results).toHaveCount(6);
    await catalog.searchFor("pixel");
    await expect(catalog.results).toHaveCount(2);
    await expect(catalog.status).toHaveText("2 devices");
    await catalog.searchFor("nokia");
    await expect(catalog.status).toHaveText("No devices match.");
  });

  test("platform filter combines with search", async ({ catalog }) => {
    await catalog.platform.selectOption("ios");
    await expect(catalog.results).toHaveCount(3);
    await catalog.searchFor("simulator");
    await expect(catalog.results).toHaveText([/iPhone 15 simulator/]);
  });

  test("reserving devices empties the cart and confirms", async ({ catalog }) => {
    await expect(catalog.checkout).toBeDisabled();
    await catalog.addToCart("Pixel 8");
    await catalog.addToCart("iPhone 16");
    await expect(catalog.cartCount).toHaveText("2");
    await catalog.checkout.click();
    await expect(catalog.confirmation).toHaveText("Reserved 2 devices.");
    await expect(catalog.cartCount).toHaveText("0");
  });
});
