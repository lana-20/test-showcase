"""Small waiting helpers on top of Vibium's auto-waiting actions.

Vibium waits for an element to be actionable before clicking or filling.
For assertions on text that changes asynchronously, these helpers poll in
the browser with wait_for_function instead of sleeping."""

from __future__ import annotations

import json

DEFAULT_TIMEOUT = 10_000


class BasePage:
    def __init__(self, page, base_url: str):
        self.page = page
        self.base_url = base_url

    def go(self, path: str) -> None:
        self.page.go(self.base_url + path)
        self.page.wait_for_load()

    def wait_text(self, testid: str, expected: str, timeout: int = DEFAULT_TIMEOUT) -> None:
        sel = json.dumps(f'[data-testid="{testid}"]')
        want = json.dumps(expected)
        self.page.wait_for_function(
            f"() => {{ const e = document.querySelector({sel}); return !!e && e.textContent.trim() === {want}; }}",
            timeout=timeout,
        )

    def wait_count(self, testid: str, count: int, timeout: int = DEFAULT_TIMEOUT) -> None:
        sel = json.dumps(f'[data-testid="{testid}"]')
        self.page.wait_for_function(
            f"() => document.querySelectorAll({sel}).length === {count}", timeout=timeout
        )

    def texts(self, testid: str) -> list[str]:
        return [e.text().strip() for e in self.page.find_all(testid=testid)]
