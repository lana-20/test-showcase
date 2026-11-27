import allure
import pytest

from pages.board import BoardPage
from pages.login import DEMO_USER, LoginPage

pytestmark = [allure.epic("Tidepool web app"), allure.feature("Tasks")]


@pytest.fixture
def board(vibe, base_url):
    LoginPage(vibe, base_url).open().sign_in(*DEMO_USER)
    return BoardPage(vibe, base_url).wait_loaded()


def test_add_tasks_and_count(board):
    board.add("Triage flaky login test", "Add Android smoke run")
    board.wait_text("remaining", "2 tasks left")
    assert board.titles() == ["Triage flaky login test", "Add Android smoke run"]


def test_complete_and_filter(board):
    board.add("Write page objects", "Pin emulator image")
    board.complete("Pin emulator image")
    board.wait_text("remaining", "1 task left")
    board.filter("Done")
    board.wait_count("task", 1)
    assert board.titles() == ["Pin emulator image"]
    board.filter("Active")
    board.wait_count("task", 1)
    assert board.titles() == ["Write page objects"]


def test_duplicate_task_is_refused(board):
    board.add("Root-cause the timeout", "ROOT-CAUSE the timeout")
    board.wait_text("task-error", "That task is already on the list.")
    board.wait_count("task", 1)
