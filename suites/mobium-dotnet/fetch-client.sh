#!/usr/bin/env bash
# Mobium's .NET client is not on NuGet yet. Check out its source at the same
# release tag as the pinned mobium binary (ci/android/install-mobium.sh), verify
# the commit, and let the solution build it as a project reference.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
TAG="${MOBIUM_VERSION:-v0.1.0-rc1}"
COMMIT="${MOBIUM_COMMIT:-bcf3501cb8381e3d465170173412c412879352ea}"
DEST="$HERE/.mobium-src"
if [ -d "$DEST/.git" ] && [ "$(git -C "$DEST" rev-parse HEAD)" = "$COMMIT" ]; then
  echo "Mobium client source already at $TAG ($COMMIT)"
  exit 0
fi
rm -rf "$DEST"
git -c advice.detachedHead=false clone --quiet --depth 1 --branch "$TAG" https://github.com/mobiumdev/mobium.git "$DEST"
actual="$(git -C "$DEST" rev-parse HEAD)"
if [ "$actual" != "$COMMIT" ]; then
  echo "Mobium $TAG is at $actual, expected $COMMIT; refusing to build it" >&2
  exit 1
fi
echo "Mobium client source: $TAG ($COMMIT)"
