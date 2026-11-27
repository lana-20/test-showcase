#!/usr/bin/env bash
# Vibium Python client (pytest) against the demo app, headless Chrome.
SUITE=vibium-python
source "$(dirname "$0")/lib/common.sh"
cd "$SUITE_DIR"
# Use the vibium binary that ships with the pinned wheel, not a developer build.
unset VIBIUM_BIN_PATH
PY="${PYTHON:-python3}"
"$PY" -c "import vibium" 2>/dev/null || "$PY" -m pip install -q -r requirements.txt
log "Running Vibium Python suite"
set +e
"$PY" -m pytest --alluredir="$RESULTS" --clean-alluredir
code=$?
set -e
finish $code
