import { expect, type Locator, type Page } from "@playwright/test";
import { step } from "allure-js-commons";

export class BoardPage {
  readonly heading: Locator;
  readonly newTask: Locator;
  readonly tasks: Locator;
  readonly remaining: Locator;
  readonly error: Locator;

  constructor(private readonly page: Page) {
    this.heading = page.getByRole("heading", { name: "Tasks" });
    this.newTask = page.getByPlaceholder("What needs testing?");
    this.tasks = page.getByTestId("task");
    this.remaining = page.getByTestId("remaining");
    this.error = page.getByTestId("task-error");
  }

  task(title: string): Locator {
    return this.tasks.filter({ hasText: title });
  }

  async add(...titles: string[]) {
    for (const title of titles) {
      await step(`Add task "${title}"`, async () => {
        await this.newTask.fill(title);
        await this.newTask.press("Enter");
      });
    }
  }

  async complete(title: string) {
    await step(`Complete "${title}"`, async () => {
      await this.page.getByRole("checkbox", { name: `Complete ${title}` }).check();
    });
  }

  async remove(title: string) {
    await step(`Delete "${title}"`, async () => {
      await this.page.getByRole("button", { name: `Delete ${title}` }).click();
    });
  }

  async filter(name: "All" | "Active" | "Done") {
    await step(`Filter: ${name}`, async () => {
      await this.page.getByRole("group", { name: "Filter tasks" }).getByRole("button", { name }).click();
    });
  }

  async expectTitles(titles: string[]) {
    await expect(this.page.getByTestId("task-title")).toHaveText(titles);
  }
}
