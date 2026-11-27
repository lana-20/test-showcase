from __future__ import annotations

import allure

from .base import BasePage


class BoardPage(BasePage):
    def wait_loaded(self) -> "BoardPage":
        self.page.wait_for_url("/board.html")
        self.page.find(role="heading", text="Tasks").wait_until("visible")
        return self

    def add(self, *titles: str) -> None:
        for title in titles:
            with allure.step(f'Add task "{title}"'):
                box = self.page.find(placeholder="What needs testing?")
                box.fill(title)
                box.press("Enter")

    def complete(self, title: str) -> None:
        with allure.step(f'Complete "{title}"'):
            self.page.find(label=f"Complete {title}").set()

    def filter(self, name: str) -> None:
        with allure.step(f"Filter: {name}"):
            self.page.find(testid=f"filter-{name.lower()}").click()

    def titles(self) -> list[str]:
        return self.texts("task-title")
