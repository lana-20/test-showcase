import allure
import pytest

from pages.board import BoardPage
from pages.login import DEMO_USER, LoginPage

pytestmark = [allure.epic("Tidepool web app"), allure.feature("Sign in")]


@pytest.mark.smoke
def test_valid_credentials_open_the_board(vibe, base_url):
    LoginPage(vibe, base_url).open().sign_in(*DEMO_USER)
    board = BoardPage(vibe, base_url).wait_loaded()
    board.wait_text("greeting", "Signed in as demo")


def test_wrong_password_is_rejected(vibe, base_url):
    login = LoginPage(vibe, base_url).open()
    login.sign_in("demo", "nope")
    assert login.error_text() == "Wrong username or password."
    assert vibe.url().endswith("index.html")


def test_protected_page_redirects_and_returns(vibe, base_url):
    login = LoginPage(vibe, base_url)
    with allure.step("Follow a link to a protected page while signed out"):
        # Open from a link click rather than page.go(): board.html redirects
        # with location.replace() while it loads, and Vibium 26.11.16 reports
        # that as "navigation canceled by concurrent navigation" on go().
        login.open()
        vibe.evaluate("location.assign('board.html')")
    vibe.wait_for_url("index.html?next=board.html")
    login.sign_in(*DEMO_USER)
    BoardPage(vibe, base_url).wait_loaded()
