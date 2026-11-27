from __future__ import annotations

import allure

from .base import BasePage


class CatalogPage(BasePage):
    def open(self, navigate: bool = True) -> "CatalogPage":
        with allure.step("Open the catalog and wait for devices to load"):
            if navigate:
                self.go("catalog.html")
            else:
                self.page.wait_for_url("/catalog.html")
            self.page.wait_for_function(
                "() => /\\d+ devices?/.test(document.querySelector('[data-testid=status]').textContent)",
                timeout=10_000,
            )
        return self

    def search(self, text: str) -> None:
        with allure.step(f'Search for "{text}"'):
            self.page.find(label="Search devices").fill(text)

    def choose_platform(self, value: str) -> None:
        with allure.step(f"Platform: {value}"):
            self.page.find(label="Platform").select_option(value)

    def add_to_cart(self, name: str) -> None:
        with allure.step(f"Add {name} to the cart"):
            self.page.find(label=f"Add {name} to cart").click()
