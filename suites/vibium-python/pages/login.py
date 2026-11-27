from __future__ import annotations

import allure

from .base import BasePage

DEMO_USER = ("demo", "tide-pass")


class LoginPage(BasePage):
    def open(self, path: str = "index.html") -> "LoginPage":
        with allure.step(f"Open {path}"):
            self.go(path)
        return self

    def sign_in(self, username: str, password: str) -> None:
        with allure.step(f'Sign in as "{username}"'):
            self.page.find(label="Username").fill(username)
            self.page.find(label="Password").fill(password)
            self.page.find(role="button", text="Sign in").click()

    def error_text(self) -> str:
        err = self.page.find(testid="login-error")
        err.wait_until("visible")
        return err.text().strip()
