package showcase.appium.support;

import java.nio.file.Path;
import java.time.Duration;

/** Settings come from the environment so any CI (or a laptop) can run the suite unchanged. */
public final class Config {
    private Config() {}

    public static String appiumUrl() {
        return env("APPIUM_URL", "http://127.0.0.1:4723");
    }

    /** Absolute path to ApiDemos-debug.apk. */
    public static String appPath() {
        return Path.of(env("APP_PATH", "apps/ApiDemos-debug.apk")).toAbsolutePath().toString();
    }

    public static String udid() {
        return System.getenv("ANDROID_SERIAL");
    }

    public static Duration explicitWait() {
        return Duration.ofSeconds(Long.parseLong(env("WAIT_SECONDS", "15")));
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }
}
