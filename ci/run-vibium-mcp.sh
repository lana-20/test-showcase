#!/usr/bin/env bash
# Vibium MCP server, driven over stdio by the Python MCP SDK.
SUITE=vibium-mcp
source "$(dirname "$0")/lib/common.sh"
cd "$SUITE_DIR"
unset VIBIUM_BIN_PATH
PY="${PYTHON:-python3}"
"$PY" -c "import vibium, mcp" 2>/dev/null || "$PY" -m pip install -q -r requirements.txt
log "Running Vibium MCP suite"
set +e
"$PY" -m pytest --alluredir="$RESULTS" --clean-alluredir
code=$?
set -e
finish $code
