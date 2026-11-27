package showcase.bidi.pages;

import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public final class LoginPage extends Page {
    public static final String USER = "demo";
    public static final String PASSWORD = "tide-pass";

    private static final By USERNAME = testId("username");
    private static final By PASSWORD_FIELD = testId("password");
    private static final By SIGN_IN = testId("sign-in");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        Allure.step("Open the sign-in page", () -> driver.get(url("index.html")));
        return this;
    }

    /** Fills the form and submits it. Where the app goes next is up to the caller to wait for. */
    public void submit(String user, String password) {
        Allure.step("Sign in as " + user, () -> {
            visible(USERNAME).sendKeys(user);
            driver.findElement(PASSWORD_FIELD).sendKeys(password);
            driver.findElement(SIGN_IN).click();
        });
    }

    /**
     * Stores the signed-in user the way the app does after a real sign-in,
     * for tests whose subject is not the sign-in form.
     */
    public static void useStoredSession(WebDriver driver) {
        Allure.step("Start signed in as " + USER + " (stored session)", () -> {
            driver.get(url("index.html"));
            ((JavascriptExecutor) driver).executeScript(
                    "localStorage.setItem('tp-user', JSON.stringify(arguments[0]))", USER);
        });
    }
}
