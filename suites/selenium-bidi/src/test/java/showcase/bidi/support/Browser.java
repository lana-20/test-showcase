package showcase.bidi.support;

import java.time.Duration;
import java.util.Arrays;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * The three browsers the suite runs on. Every one asks for a WebDriver BiDi
 * session ({@code webSocketUrl: true}); nothing here touches CDP.
 */
public enum Browser {
    CHROME("chrome", "Chrome"),
    FIREFOX("firefox", "Firefox"),
    EDGE("edge", "Edge");

    private final String id;
    private final String displayName;

    Browser(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Capabilities options() {
        boolean headless = Config.headless() && Config.gridUrl().isEmpty();
        AbstractDriverOptions<?> options = switch (this) {
            case CHROME -> {
                ChromeOptions o = new ChromeOptions().enableBiDi();
                if (headless) o.addArguments("--headless=new");
                yield o.addArguments("--window-size=1280,900");
            }
            case EDGE -> {
                EdgeOptions o = new EdgeOptions().enableBiDi();
                if (headless) o.addArguments("--headless=new");
                yield o.addArguments("--window-size=1280,900");
            }
            case FIREFOX -> {
                FirefoxOptions o = new FirefoxOptions().enableBiDi();
                if (headless) o.addArguments("-headless");
                yield o.addArguments("-width=1280", "-height=900");
            }
        };
        // A hung navigation (say, an unanswered auth challenge) fails fast instead of after 5 minutes.
        return options.setPageLoadTimeout(Duration.ofSeconds(30));
    }

    public static Browser fromId(String id) {
        return Arrays.stream(values())
                .filter(b -> b.id.equalsIgnoreCase(id) || b.name().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown browser in BROWSERS: " + id));
    }
}
