#!/usr/bin/env bash
# Install the pinned Mobium release binary (linux/amd64) and verify it against SHA256SUMS.
set -euo pipefail
VERSION="${MOBIUM_VERSION:-v0.1.0-rc1}"
V="${VERSION#v}"
DEST="${MOBIUM_INSTALL_DIR:-$HOME/.local/bin}"
TMP="$(mktemp -d)"
ASSET="mobium_${V}_linux_amd64.tar.gz"
BASE="https://github.com/mobiumdev/mobium/releases/download/$VERSION"
curl -fsSL -o "$TMP/$ASSET" "$BASE/$ASSET"
curl -fsSL -o "$TMP/SHA256SUMS" "$BASE/SHA256SUMS"
(cd "$TMP" && grep " $ASSET\$" SHA256SUMS | sha256sum -c -)
mkdir -p "$DEST"
tar -xzf "$TMP/$ASSET" -C "$TMP"
install -m 0755 "$(find "$TMP" -type f -name mobium | head -1)" "$DEST/mobium"
echo "$DEST" >> "${GITHUB_PATH:-/dev/null}"
"$DEST/mobium" --version
