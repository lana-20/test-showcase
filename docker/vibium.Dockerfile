# Vibium suites (Python client, or MCP server driven over stdio).
#   docker build -f docker/vibium.Dockerfile --build-arg SUITE=vibium-python .
#   docker build -f docker/vibium.Dockerfile --build-arg SUITE=vibium-mcp .
FROM python:3.12.16-slim-bookworm
ARG SUITE=vibium-python
ENV PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    CI=true \
    SHOWCASE_SUITE=${SUITE}
RUN useradd --create-home --uid 1001 tester
COPY suites/${SUITE}/requirements.txt /tmp/requirements.txt
RUN pip install -r /tmp/requirements.txt && rm /tmp/requirements.txt
# Vibium downloads Chrome for Testing into the user's cache; then install the
# system libraries that Chrome lists in its own deb.deps file.
USER tester
RUN vibium install
USER root
RUN set -eux; \
    deps="$(find /home/tester/.cache -name deb.deps -print -quit)"; \
    apt-get update; \
    if [ -n "$deps" ]; then sed -E 's/ *\(.*//; s/ *\|.*//' "$deps" | xargs apt-get install -y --no-install-recommends; fi; \
    apt-get install -y --no-install-recommends fonts-liberation ca-certificates; \
    rm -rf /var/lib/apt/lists/*
WORKDIR /work
RUN chown tester:tester /work
COPY --chown=tester:tester ci/ ci/
COPY --chown=tester:tester demo-app/ demo-app/
COPY --chown=tester:tester suites/${SUITE}/ suites/${SUITE}/
ARG IMAGE_REF=""
ENV SHOWCASE_IMAGE=${IMAGE_REF}
USER tester
CMD ["sh", "-c", "exec ci/run-${SHOWCASE_SUITE}.sh"]
