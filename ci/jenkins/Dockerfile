# Jenkinsfile Runner (a real Jenkins, run once per build, no server) plus what
# the Appium Java pipeline needs: a JDK to compile the tests and Python for
# the status file. The Android emulator and Appium stay on the host.
FROM ghcr.io/jenkinsci/jenkinsfile-runner:jre-21@sha256:1fdf38e99b60286b12356ae165d4539c626b9f43fcf78aca208cf8fc5585cd37
USER root
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends openjdk-21-jdk-headless python3 curl \
 && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
    PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
    GRADLE_USER_HOME=/opt/gradle-home
# Bake the Gradle distribution (pinned and checksum-verified by the wrapper)
# and every test dependency into GRADLE_USER_HOME. Only the build files are
# copied, so this layer is rebuilt only when they change, and a pipeline run
# needs no downloads.
COPY suites/appium-java/gradlew suites/appium-java/settings.gradle.kts suites/appium-java/build.gradle.kts /tmp/appium-java/
COPY suites/appium-java/gradle/ /tmp/appium-java/gradle/
RUN cd /tmp/appium-java \
 && ./gradlew --no-daemon --console=plain downloadDependencies \
 && rm -rf /tmp/appium-java "$GRADLE_USER_HOME/daemon"
ARG IMAGE_REF=""
ENV SHOWCASE_IMAGE=${IMAGE_REF}
