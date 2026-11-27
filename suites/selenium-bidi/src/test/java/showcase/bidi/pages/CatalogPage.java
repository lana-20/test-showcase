package showcase.bidi.pages;

import io.qameta.allure.Allure;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public final class CatalogPage extends Page {
    public static final String DEVICES_PATH = "devices.json";

    private static final By HEADING = By.cssSelector("h1#title");
    private static final By STATUS = testId("status");
    private static final By RESULT = testId("result");
    private static final By SEARCH = testId("search");
    private static final By PLATFORM = testId("platform");

    public CatalogPage(WebDriver driver) {
        super(driver);
    }

    /** Opens the page; the device list loads afterwards, from {@value #DEVICES_PATH}. */
    public CatalogPage open() {
        Allure.step("Open the device catalog", () -> {
            driver.get(url("catalog.html"));
            wait.until(ExpectedConditions.textToBe(HEADING, "Device catalog"));
        });
        return this;
    }

    public String status() {
        return visible(STATUS).getText();
    }

    public CatalogPage waitForStatus(String text) {
        waitForText(STATUS, text);
        return this;
    }

    public List<String> deviceNames() {
        return driver.findElements(RESULT).stream()
                .map(e -> e.findElement(By.tagName("span")).getText())
                .toList();
    }

    public CatalogPage search(String text) {
        Allure.step("Search for \"" + text + "\"", () -> {
            WebElement box = visible(SEARCH);
            box.clear();
            box.sendKeys(text);
        });
        return this;
    }

    public CatalogPage platform(String value) {
        Allure.step("Filter by platform " + value, () -> new Select(visible(PLATFORM)).selectByValue(value));
        return this;
    }

    public WebElement results() {
        return visible(By.cssSelector("[data-testid='results']"));
    }
}
