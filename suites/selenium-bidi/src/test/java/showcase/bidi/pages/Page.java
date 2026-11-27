package showcase.bidi.pages;

import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import showcase.bidi.support.Config;

/** Shared plumbing for the Tidepool page objects: test-id locators and explicit waits. */
public abstract class Page {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected Page(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Config.timeout());
    }

    public static String url(String path) {
        return Config.demoUrl() + path;
    }

    protected static By testId(String id) {
        return By.cssSelector("[data-testid='" + id + "']");
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void waitForText(By locator, String text) {
        Allure.step("Wait for \"" + text + "\"", () -> {
            wait.withMessage(() -> "text of " + locator + " was \"" + driver.findElement(locator).getText() + "\"")
                    .until(ExpectedConditions.textToBe(locator, text));
        });
    }
}
