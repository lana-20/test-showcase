# Sourced by the Kernel run scripts. kernel_create starts one headless Kernel
# browser and registers a trap that deletes it on exit (closing a Playwright
# connection does not delete a Kernel browser). The URLs carry the session's
# auth, so they are masked in GitHub Actions logs and never printed.
KERNEL_API="${KERNEL_API:-https://api.onkernel.com}"

kernel_delete() {
  if [ -n "${KERNEL_SESSION_ID:-}" ]; then
    curl -fsS -X DELETE -H "Authorization: Bearer $KERNEL_API_KEY" \
      "$KERNEL_API/browsers/$KERNEL_SESSION_ID" >/dev/null && echo "[kernel] deleted $KERNEL_SESSION_ID" \
      || echo "[kernel] delete failed for $KERNEL_SESSION_ID; it times out on Kernel's side"
  fi
}

kernel_create() {
  local resp
  # timeout_seconds: Kernel deletes the browser after this long with no client attached.
  resp="$(curl -fsS -X POST "$KERNEL_API/browsers" \
    -H "Authorization: Bearer $KERNEL_API_KEY" -H "Content-Type: application/json" \
    -d '{"headless": true, "timeout_seconds": 120}')"
  KERNEL_SESSION_ID="$(python3 -c 'import json,sys;print(json.load(sys.stdin)["session_id"])' <<<"$resp")"
  KERNEL_CDP_URL="$(python3 -c 'import json,sys;print(json.load(sys.stdin)["cdp_ws_url"])' <<<"$resp")"
  KERNEL_BIDI_URL="$(python3 -c 'import json,sys;print(json.load(sys.stdin).get("webdriver_ws_url",""))' <<<"$resp")"
  if [ -n "${GITHUB_ACTIONS:-}" ]; then
    echo "::add-mask::$KERNEL_CDP_URL"
    [ -n "$KERNEL_BIDI_URL" ] && echo "::add-mask::$KERNEL_BIDI_URL"
  fi
  export KERNEL_SESSION_ID KERNEL_CDP_URL KERNEL_BIDI_URL
  echo "[kernel] created browser session $KERNEL_SESSION_ID"
  trap 'code=$?; kernel_delete; exit $code' EXIT
}
