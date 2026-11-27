"""One Mobium session on the Android emulator for the whole run; each test
starts from a freshly launched ApiDemos."""

from __future__ import annotations

import os
import pathlib
import shutil
import subprocess

import allure
import pytest
from mobium import start

PACKAGE = "io.appium.android.apis"
APK = pathlib.Path(os.environ.get("APP_PATH", "apps/ApiDemos-debug.apk")).resolve()


def adb(*args: str) -> str:
    """adb for the two things Mobium leaves to the platform here: opening an
    activity by name, and reading the package list in a sanity check."""
    cmd = [shutil.which("adb") or "adb"]
    if os.environ.get("ANDROID_SERIAL"):
        cmd += ["-s", os.environ["ANDROID_SERIAL"]]
    return subprocess.run(cmd + list(args), check=True, capture_output=True, text=True, timeout=60).stdout


class StepDevice:
    """Wraps a Mobium Device so every call shows up as an Allure step."""

    def __init__(self, device):
        self._device = device

    def __getattr__(self, name):
        attr = getattr(self._device, name)
        if not callable(attr):
            return attr

        def stepped(*args, **kwargs):
            shown = ", ".join([repr(a) for a in args] + [f"{k}={v!r}" for k, v in kwargs.items()])
            with allure.step(f"mobium {name}({shown})"):
                return attr(*args, **kwargs)

        return stepped


@pytest.fixture(scope="session")
def device():
    assert APK.exists(), f"ApiDemos APK not found at {APK}; set APP_PATH"
    with allure.step("Start a Mobium session on Android"):
        d = start(platform="android", device=os.environ.get("ANDROID_SERIAL"), call_timeout=300)
    try:
        with allure.step(f"Install {APK.name}"):
            d.install(str(APK))
        yield StepDevice(d)
    finally:
        d.quit()


@pytest.fixture
def app(device, request):
    """ApiDemos, launched fresh for each test."""
    device.terminate(PACKAGE)
    device.launch(PACKAGE)
    device.wait_for('text="Animation"')
    yield device
    rep = getattr(request.node, "rep_call", None)
    if rep is not None and rep.failed:
        try:
            allure.attach(device._device.screenshot(), name="screenshot on failure",
                          attachment_type=allure.attachment_type.PNG)
            allure.attach("\n".join(str(e) for e in device._device.map()), name="map on failure",
                          attachment_type=allure.attachment_type.TEXT)
        except Exception as exc:
            allure.attach(repr(exc), name="screenshot unavailable", attachment_type=allure.attachment_type.TEXT)


@pytest.fixture
def open_activity(app):
    def _open(activity: str, ready: str):
        with allure.step(f"Open {activity}"):
            adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/{activity}")
            app.wait_for(ready)
        return app
    return _open


@pytest.hookimpl(hookwrapper=True, tryfirst=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    rep = outcome.get_result()
    setattr(item, "rep_" + rep.when, rep)
