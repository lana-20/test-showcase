package showcase.appium.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** Views > Controls > 1. Light Theme. */
public final class ControlsScreen extends Screen {
    public static final By EDIT = id("edit");
    public static final By CHECK_1 = id("check1");
    public static final By RADIO_1 = id("radio1");
    public static final By RADIO_2 = id("radio2");
    public static final By TOGGLE_1 = id("toggle1");
    public static final By SAVE_DISABLED = id("button_disabled");

    public ControlsScreen(AndroidDriver driver) {
        super(driver);
    }

    public ControlsScreen open() {
        startActivity(".view.Controls1");
        return waitUntilShown();
    }

    public ControlsScreen waitUntilShown() {
        visible(EDIT);
        return this;
    }

    public void typeText(String text) {
        Allure.step("Type \"" + text + "\" into the text field", () -> {
            WebElement edit = clickable(EDIT);
            edit.clear();
            edit.sendKeys(text);
        });
    }

    public String text() {
        return visible(EDIT).getText();
    }

    public void tap(By control, String name) {
        Allure.step("Tap " + name, () -> clickable(control).click());
    }

    public boolean isChecked(By control) {
        return Boolean.parseBoolean(visible(control).getAttribute("checked"));
    }

    public String label(By control) {
        return visible(control).getText();
    }

    public boolean isEnabled(By control) {
        return visible(control).isEnabled();
    }

    public void hideKeyboardIfShown() {
        if (driver.isKeyboardShown()) {
            driver.hideKeyboard();
        }
    }

    public static By byText(String text) {
        return AppiumBy.androidUIAutomator("new UiSelector().text(\"" + text + "\")");
    }
}
