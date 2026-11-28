// Appium Java suite (ApiDemos on an Android emulator). Jenkins runs it with
// `./gradlew test --no-daemon -Dallure.results.directory=<dir>` (ci/run-appium-java.sh).
plugins {
    java
}

group = "showcase"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(platform(libs.allure.bom))
    testImplementation(libs.appium.java.client)
    testImplementation(libs.selenium.support)
    testImplementation(libs.selenium.remote.driver)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.allure.jupiter)
    testImplementation(libs.slf4j.simple)
    // Gradle 9 no longer puts a JUnit Platform launcher on the test runtime classpath.
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// Results go where the run script asks (-Dallure.results.directory=...), else
// to ./allure-results, as under Maven. The tests only use Allure's lambda API
// (Allure.step, Allure.attachment), so no AspectJ agent is needed.
val allureResults: Provider<String> = providers.systemProperty("allure.results.directory")
    .orElse(layout.projectDirectory.dir("allure-results").asFile.path)

tasks.test {
    useJUnitPlatform()
    // Surefire's default includes, so the same classes run as under Maven.
    include("**/Test*.class", "**/*Test.class", "**/*Tests.class", "**/*TestCase.class")
    systemProperty("allure.results.directory", allureResults.get())
    // These tests drive a live device: never skip them as up to date or take them from a cache.
    doNotTrackState("End-to-end tests against a live Android device")
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// Downloads every jar the tests compile and run against. The Jenkins runner
// image runs this at build time, so a pipeline run needs no downloads.
tasks.register("downloadDependencies") {
    description = "Resolves the test compile and runtime classpaths into the Gradle user home."
    val classpaths = files(configurations.testCompileClasspath, configurations.testRuntimeClasspath)
    inputs.files(classpaths)
    doLast { logger.lifecycle("Resolved ${classpaths.files.size} files") }
}
