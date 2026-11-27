"""Fixtures: a local copy of the demo app, one headless browser per session,
and a fresh browser context (clean storage) per test."""

from __future__ import annotations

import functools
import http.server
import os
import pathlib
import threading

import allure
import pytest
from vibium import browser as vibium_browser

DEMO_DIR = pathlib.Path(__file__).resolve().parents[2] / "demo-app"


class _QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, *args):  # keep the test log readable
        pass


@pytest.fixture(scope="session")
def base_url():
    """Serve demo-app/ on a free port, unless DEMO_URL points at one already."""
    if os.environ.get("DEMO_URL"):
        yield os.environ["DEMO_URL"].rstrip("/") + "/"
        return
    handler = functools.partial(_QuietHandler, directory=str(DEMO_DIR))
    server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    yield f"http://127.0.0.1:{server.server_port}/"
    server.shutdown()


@pytest.fixture(scope="session")
def bro():
    headless = os.environ.get("HEADED") != "1"
    b = vibium_browser.start(headless=headless)
    yield b
    b.stop()


@pytest.fixture
def vibe(bro, request):
    context = bro.new_context()
    page = context.new_page()
    yield page
    failed = getattr(request.node, "rep_call", None)
    if failed is not None and failed.failed:
        try:
            allure.attach(page.screenshot(), name="screenshot on failure",
                          attachment_type=allure.attachment_type.PNG)
            allure.attach(page.url, name="url", attachment_type=allure.attachment_type.TEXT)
        except Exception as exc:  # the browser may be the thing that failed
            allure.attach(repr(exc), name="screenshot unavailable",
                          attachment_type=allure.attachment_type.TEXT)
    context.close()


@pytest.hookimpl(hookwrapper=True, tryfirst=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    rep = outcome.get_result()
    setattr(item, "rep_" + rep.when, rep)
