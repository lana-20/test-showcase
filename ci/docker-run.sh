#!/usr/bin/env bash
# Run one suite inside its published image, the same way on any CI with Docker.
#   ci/docker-run.sh <suite> [extra docker run flags...]
# Image: $REGISTRY/test-showcase-<suite>:$TAG (defaults: ghcr.io/lana-20, latest).
# Results: ./out/allure-results/<suite> and ./out/status/<suite>.json.
set -euo pipefail
SUITE="$1"; shift
IMAGE="${REGISTRY:-ghcr.io/lana-20}/test-showcase-${SUITE}:${TAG:-latest}"
mkdir -p out && chmod 777 out
# Forward the variables ci/lib/status.py uses to say which CI ran this.
FORWARD=(GITHUB_ACTIONS GITHUB_SERVER_URL GITHUB_REPOSITORY GITHUB_RUN_ID GITHUB_SHA
         GITLAB_CI CI_JOB_URL CI_COMMIT_SHA
         TF_BUILD SYSTEM_COLLECTIONURI SYSTEM_TEAMPROJECT BUILD_BUILDID BUILD_SOURCEVERSION
         CIRCLECI CIRCLE_BUILD_URL CIRCLE_SHA1 HOST_CI_NAME HOST_RUN_URL)
ENV_ARGS=()
for v in "${FORWARD[@]}"; do [ -n "${!v:-}" ] && ENV_ARGS+=(-e "$v"); done
echo "Running $SUITE in $IMAGE"
docker run --rm "${ENV_ARGS[@]}" -e OUT_DIR=/work/out -v "$PWD/out:/work/out" "$@" "$IMAGE"
