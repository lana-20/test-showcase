# Playwright suite. The base image already has Chromium and its system
# libraries for this exact Playwright version.
FROM mcr.microsoft.com/playwright:v1.65.1-noble
ENV CI=true \
    SHOWCASE_SUITE=playwright
# python3 writes the status JSON (ci/lib/status.py).
RUN command -v python3 || (apt-get update && apt-get install -y --no-install-recommends python3 \
    && rm -rf /var/lib/apt/lists/*)
WORKDIR /work
RUN chown pwuser:pwuser /work
# Dependencies first, so the npm layer is cached until package-lock.json changes.
COPY --chown=pwuser:pwuser suites/playwright/package.json suites/playwright/package-lock.json suites/playwright/
RUN cd suites/playwright && npm ci --no-audit --no-fund && npm cache clean --force
COPY --chown=pwuser:pwuser ci/ ci/
COPY --chown=pwuser:pwuser demo-app/ demo-app/
COPY --chown=pwuser:pwuser suites/playwright/ suites/playwright/
ARG IMAGE_REF=""
ENV SHOWCASE_IMAGE=${IMAGE_REF}
USER pwuser
CMD ["ci/run-playwright.sh"]
