package showcase.appium.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import showcase.appium.support.Config;
import showcase.appium.support.DriverFactory;

/** Shared helpers: explicit waits only, never sleeps. */
public abstract class Screen {
    protected final AndroidDriver driver;
    protected final WebDriverWait wait;

    protected Screen(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Config.explicitWait());
    }

    protected static By id(String resourceId) {
        return AppiumBy.id(DriverFactory.APP_PACKAGE + ":id/" + resourceId);
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement clickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Open an activity directly, the fast and deterministic way into a screen. */
    protected void startActivity(String activity) {
        Allure.step("Start activity " + activity, () ->
                driver.executeScript("mobile: startActivity", Map.of(
                        "intent", DriverFactory.APP_PACKAGE + "/" + activity,
                        "wait", true,
                        "stop", true)));
    }

    /** Scroll a list until an item with this exact text is on screen, then tap it. */
    protected void tapListItem(String text) {
        Allure.step("Tap list item \"" + text + "\"", () -> {
            By item = AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView("
                            + "new UiSelector().text(\"" + text + "\"))");
            clickable(item).click();
        });
    }
}
