package showcase.bidi.pages;

import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.browsingcontext.BrowsingContext;
import org.openqa.selenium.bidi.browsingcontext.ReadinessState;

/** The one route behind HTTP Basic auth (served by demo-app/serve.mjs). */
public final class LabPage extends Page {
    public static final String PATH = "lab";
    private static final By STATUS = testId("lab-status");

    public LabPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Navigates with BiDi browsingContext.navigate. A classic driver.get() can
     * block in chromedriver while the auth challenge is pending, before the BiDi
     * handler has had a chance to answer it.
     */
    public LabPage open() {
        Allure.step("Open the lab console (Basic auth)", () ->
                new BrowsingContext(driver, driver.getWindowHandle()).navigate(url(PATH), ReadinessState.COMPLETE));
        return this;
    }

    public String status() {
        return visible(STATUS).getText();
    }

    public String bodyText() {
        return visible(By.tagName("body")).getText();
    }
}
