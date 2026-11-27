package showcase.appium.support;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;

/**
 * One Appium session per test class (session start is the slow part), with a
 * screenshot and the page source attached to Allure when a test fails.
 * Tests receive the driver as an {@code AndroidDriver} parameter.
 */
public final class AppiumExtension implements BeforeAllCallback, AfterAllCallback, TestWatcher, ParameterResolver {
    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(AppiumExtension.class);

    public static AndroidDriver driver(ExtensionContext context) {
        return context.getRoot().getStore(NS).get(context.getRequiredTestClass(), AndroidDriver.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        AndroidDriver driver = Allure.step("Start an Appium UiAutomator2 session", DriverFactory::create);
        context.getRoot().getStore(NS).put(context.getRequiredTestClass(), driver);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        AndroidDriver driver = context.getRoot().getStore(NS).remove(context.getRequiredTestClass(), AndroidDriver.class);
        if (driver != null) {
            driver.quit();
        }
    }

    /** Lets tests and lifecycle methods take the class's driver as a parameter. */
    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == AndroidDriver.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        return driver(context);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        AndroidDriver driver = driver(context);
        if (driver == null) {
            return;
        }
        try {
            byte[] png = driver.getScreenshotAs(OutputType.BYTES);
            Allure.attachment("screenshot on failure", "image/png", new ByteArrayInputStream(png),
                    AttachmentOptions.withFileExtension("png"));
            Allure.attachment("page source on failure", "text/xml", driver.getPageSource(),
                    AttachmentOptions.withFileExtension("xml"));
        } catch (RuntimeException e) {
            Allure.attachment("screenshot unavailable", e.toString());
        }
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {}
}
