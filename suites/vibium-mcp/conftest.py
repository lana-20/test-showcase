from __future__ import annotations

import functools
import http.server
import os
import pathlib
import threading

import pytest

DEMO_DIR = pathlib.Path(__file__).resolve().parents[2] / "demo-app"


class _QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, *args):
        pass


@pytest.fixture(scope="session")
def base_url():
    if os.environ.get("DEMO_URL"):
        yield os.environ["DEMO_URL"].rstrip("/") + "/"
        return
    handler = functools.partial(_QuietHandler, directory=str(DEMO_DIR))
    server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    yield f"http://127.0.0.1:{server.server_port}/"
    server.shutdown()
