"""Mobium driving ApiDemos on an Android emulator through its Python client."""

import allure
import pytest
from mobium import MobiumError, NoSuchElementError

from conftest import PACKAGE, adb

pytestmark = [allure.epic("Mobium on Android"), allure.feature("ApiDemos")]


def labels(elements):
    return [e.label for e in elements]


def test_session_is_on_an_android_emulator(device):
    assert device.session.platform == "android"
    emulators = [d for d in device.devices() if d.platform == "android" and d.emulator]
    assert emulators, "no Android emulator listed by mobium devices"
    assert PACKAGE in adb("shell", "pm", "list", "packages", PACKAGE)


def test_map_lists_the_home_categories_with_refs(app):
    elements = app.map()
    found = labels(elements)
    for category in ("Animation", "App", "Content"):
        assert any(label.startswith(category) for label in found), f"{category} not in {found}"
    assert all(e.ref.startswith("@e") for e in elements)


def test_tap_scrolls_to_off_screen_rows(app):
    # "Views" sits below the fold; tap scrolls to it before touching anything.
    app.tap('text="Views"')
    app.tap('text="Controls"')
    app.tap('text="1. Light Theme"')
    app.wait_for("testid=edit")


def test_fill_replaces_the_field_value(open_activity):
    app = open_activity(".view.Controls1", "testid=edit")
    app.fill("testid=edit", "Mobium was here")
    app.wait_for("testid=edit", condition="value", text="Mobium was here")
    app.fill("testid=edit", "and replaced it")
    app.wait_for("testid=edit", condition="value", text="and replaced it")


def test_set_and_unset_a_checkbox(open_activity):
    app = open_activity(".view.Controls1", "testid=check1")
    app.set("testid=check1")
    app.wait_for("testid=check1", condition="checked")
    app.unset("testid=check1")
    app.wait_for("testid=check1", condition="unchecked")


def test_map_diff_reports_what_a_tap_changed(open_activity):
    app = open_activity(".view.Controls1", "testid=toggle1")
    app.map()
    app.tap("testid=toggle1")
    app.wait_for("testid=toggle1", condition="checked")
    diff = app.map_diff()
    changed = [c for c in diff["changed"] if "toggle1" in c["after"].locator or c["after"].label in ("ON", "OFF")]
    assert changed, f"toggle change not in diff: {diff['changed']}"


def test_dialog_is_read_and_answered(open_activity):
    app = open_activity(".app.AlertDialogSamples", "testid=two_buttons")
    app.tap("testid=two_buttons")
    app.wait_for('text="OK"')
    assert "Lorem ipsum" in app.text()
    app.tap('text="OK"')
    app.wait_for('text="OK"', condition="hidden")
    app.wait_for("testid=two_buttons")


def test_missing_element_is_refused_with_a_remedy(app):
    with pytest.raises(NoSuchElementError) as err:
        app.tap("testid=noSuchButton")
    assert isinstance(err.value, MobiumError)
    assert "noSuchButton" in str(err.value)


def test_screenshot_is_a_png(app):
    png = app.screenshot()
    assert png[:8] == b"\x89PNG\r\n\x1a\n"
    allure.attach(png, name="home screen", attachment_type=allure.attachment_type.PNG)
