package showcase.bidi.support;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Settings come from the environment, so a laptop, Compose and the Kubernetes Job run the suite unchanged. */
public final class Config {
    private Config() {}

    /** The demo app as the browser sees it, always ending in a slash. */
    public static String demoUrl() {
        String url = env("DEMO_URL", "http://127.0.0.1:8080/");
        return url.endsWith("/") ? url : url + "/";
    }

    /** The Selenium Grid. Without one, the browsers start on this machine (Selenium Manager finds the drivers). */
    public static Optional<URL> gridUrl() {
        String url = env("SELENIUM_REMOTE_URL", "");
        if (url.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(URI.create(url).toURL());
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Bad SELENIUM_REMOTE_URL: " + url, e);
        }
    }

    /** Which browsers each test class runs on, e.g. BROWSERS=chrome,firefox. */
    public static List<Browser> browsers() {
        return Arrays.stream(env("BROWSERS", "chrome,firefox,edge").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Browser::fromId)
                .toList();
    }

    public static Duration timeout() {
        return Duration.ofSeconds(Long.parseLong(env("WAIT_SECONDS", "15")));
    }

    public static boolean headless() {
        return !env("HEADLESS", "1").equals("0");
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v.trim();
    }
}
