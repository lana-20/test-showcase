#!/usr/bin/env bash
# A cold-booted CI emulator often raises "Pixel Launcher isn't responding", and
# the dialog covers the app under test. Hide system error and ANR dialogs (the
# system then closes a stalled app instead of asking), and dismiss any already up.
# Best effort: never fails the run.
adb shell settings put global hide_error_dialogs 1 || true
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
