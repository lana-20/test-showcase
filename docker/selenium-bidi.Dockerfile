# Selenium WebDriver BiDi suite. The image holds only the tests and a JDK:
# the browsers live on the Selenium Grid nodes (SELENIUM_REMOTE_URL).
FROM eclipse-temurin:21.0.13.1_2-jdk-noble
ENV CI=true \
    SHOWCASE_SUITE=selenium-bidi
# python3 writes the status JSON (ci/lib/status.py).
RUN apt-get update && apt-get install -y --no-install-recommends python3 && rm -rf /var/lib/apt/lists/*
RUN useradd --create-home --uid 1001 tester
WORKDIR /work
RUN chown tester:tester /work
USER tester
# Maven and the dependencies first, so this layer is cached until pom.xml changes.
COPY --chown=tester:tester suites/selenium-bidi/pom.xml suites/selenium-bidi/mvnw suites/selenium-bidi/
COPY --chown=tester:tester suites/selenium-bidi/.mvn/ suites/selenium-bidi/.mvn/
RUN cd suites/selenium-bidi && ./mvnw -B -ntp -q dependency:go-offline
COPY --chown=tester:tester ci/ ci/
COPY --chown=tester:tester suites/selenium-bidi/src/ suites/selenium-bidi/src/
# Compile now, and let Surefire fetch its JUnit provider, so a run needs no downloads.
RUN cd suites/selenium-bidi && ./mvnw -B -ntp -q test -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false
ARG IMAGE_REF=""
ENV SHOWCASE_IMAGE=${IMAGE_REF}
CMD ["ci/run-selenium-bidi.sh"]
