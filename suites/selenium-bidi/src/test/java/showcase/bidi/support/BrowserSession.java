package showcase.bidi.support;

import io.qameta.allure.Allure;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.browsingcontext.BrowsingContext;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * A fresh BiDi session for every test, on the Grid when SELENIUM_REMOTE_URL is
 * set and on this machine otherwise. Tests and lifecycle methods take what they
 * need as parameters: {@link RemoteWebDriver} (or {@link WebDriver}),
 * {@link BrowsingContext} for the current tab, and the {@link Browser}.
 * On a failure the BiDi screenshot and the URL are attached to Allure while the
 * session is still open.
 */
public final class BrowserSession
        implements BeforeEachCallback, AfterTestExecutionCallback, AfterEachCallback, ParameterResolver {
    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(BrowserSession.class);

    private final Browser browser;

    BrowserSession(Browser browser) {
        this.browser = browser;
    }

    public static RemoteWebDriver driver(ExtensionContext context) {
        return context.getStore(NS).get(RemoteWebDriver.class, RemoteWebDriver.class);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        Allure.label("parentSuite", browser.displayName());
        Allure.parameter("browser", browser.displayName());
        String where = Config.gridUrl().map(u -> "on the Grid").orElse("locally");
        RemoteWebDriver driver = Allure.step("Start a " + browser.displayName() + " session with WebDriver BiDi " + where,
                () -> start(browser.options()));
        context.getStore(NS).put(RemoteWebDriver.class, driver);
        Object ws = driver.getCapabilities().getCapability("webSocketUrl");
        if (!(ws instanceof String)) {
            throw new IllegalStateException(browser.displayName() + " returned no webSocketUrl: BiDi is not available");
        }
        Capabilities caps = driver.getCapabilities();
        Allure.parameter("version", caps.getBrowserVersion());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isEmpty()) {
            return;
        }
        RemoteWebDriver driver = driver(context);
        if (driver == null) {
            return;
        }
        try {
            BrowsingContext tab = new BrowsingContext(driver, driver.getWindowHandle());
            Attachments.png("screenshot on failure", tab.captureScreenshot());
            Allure.addAttachment("URL on failure", driver.getCurrentUrl());
        } catch (RuntimeException e) {
            Allure.addAttachment("screenshot unavailable", e.toString());
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        RemoteWebDriver driver = context.getStore(NS).remove(RemoteWebDriver.class, RemoteWebDriver.class);
        if (driver != null) {
            driver.quit();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        Class<?> type = parameter.getParameter().getType();
        return type == RemoteWebDriver.class || type == WebDriver.class || type == BrowsingContext.class
                || type == Browser.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        Class<?> type = parameter.getParameter().getType();
        if (type == Browser.class) {
            return browser;
        }
        RemoteWebDriver driver = Optional.ofNullable(driver(context))
                .orElseThrow(() -> new IllegalStateException("No browser session: inject it into tests or @BeforeEach only"));
        if (type == BrowsingContext.class) {
            return new BrowsingContext(driver, driver.getWindowHandle());
        }
        return driver;
    }

    private static RemoteWebDriver start(Capabilities options) {
        if (Config.gridUrl().isPresent()) {
            return new RemoteWebDriver(Config.gridUrl().get(), options);
        }
        return switch (options) {
            case ChromeOptions o -> new ChromeDriver(o);
            case EdgeOptions o -> new EdgeDriver(o);
            case FirefoxOptions o -> new FirefoxDriver(o);
            default -> throw new IllegalArgumentException("No local driver for " + options.getBrowserName());
        };
    }
}
