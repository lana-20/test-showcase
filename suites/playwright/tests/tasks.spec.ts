import { epic, feature } from "allure-js-commons";
import { expect, test } from "./fixtures";

test.describe("Task board", () => {
  test.beforeEach(async ({ signedIn }) => {
    void signedIn;
    await epic("Tidepool web app");
    await feature("Tasks");
  });

  test("adding tasks updates the list and the counter", async ({ board }) => {
    await board.add("Triage flaky login test", "Add Android smoke run");
    await board.expectTitles(["Triage flaky login test", "Add Android smoke run"]);
    await expect(board.remaining).toHaveText("2 tasks left");
  });

  test("completing and filtering tasks", async ({ board }) => {
    await board.add("Write page objects", "Pin emulator image", "Publish Allure report");
    await board.complete("Pin emulator image");
    await expect(board.remaining).toHaveText("2 tasks left");

    await board.filter("Done");
    await board.expectTitles(["Pin emulator image"]);
    await board.filter("Active");
    await board.expectTitles(["Write page objects", "Publish Allure report"]);
    await board.filter("All");
    await expect(board.tasks).toHaveCount(3);
  });

  test("duplicate and blank tasks are refused", async ({ board }) => {
    await board.add("Root-cause the timeout");
    await board.add("root-cause the TIMEOUT");
    await expect(board.error).toHaveText("That task is already on the list.");
    await board.add("   ");
    await expect(board.error).toHaveText("A task needs a title.");
    await expect(board.tasks).toHaveCount(1);
  });

  test("tasks survive a reload and can be deleted", async ({ page, board }) => {
    await board.add("Keep me", "Delete me");
    await page.reload();
    await board.expectTitles(["Keep me", "Delete me"]);
    await board.remove("Delete me");
    await board.expectTitles(["Keep me"]);
    await expect(board.remaining).toHaveText("1 task left");
  });
});
