package showcase.appium.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** App > Alert Dialogs. */
public final class AlertDialogsScreen extends Screen {
    private static final By OK_CANCEL = id("two_buttons");
    private static final By TEXT_ENTRY = id("text_entry_button");
    private static final By DIALOG_TITLE = AppiumBy.id("android:id/alertTitle");
    private static final By POSITIVE = AppiumBy.id("android:id/button1");
    private static final By NEGATIVE = AppiumBy.id("android:id/button2");

    public AlertDialogsScreen(AndroidDriver driver) {
        super(driver);
    }

    public AlertDialogsScreen open() {
        startActivity(".app.AlertDialogSamples");
        visible(OK_CANCEL);
        return this;
    }

    public void openOkCancelDialog() {
        Allure.step("Open the OK/Cancel dialog", () -> clickable(OK_CANCEL).click());
    }

    public void openTextEntryDialog() {
        Allure.step("Open the text entry dialog", () -> clickable(TEXT_ENTRY).click());
    }

    public String dialogTitle() {
        return visible(DIALOG_TITLE).getText();
    }

    public String positiveLabel() {
        return visible(POSITIVE).getText();
    }

    public String negativeLabel() {
        return visible(NEGATIVE).getText();
    }

    public void accept() {
        Allure.step("Tap OK", () -> clickable(POSITIVE).click());
    }

    public void dismiss() {
        Allure.step("Tap Cancel", () -> clickable(NEGATIVE).click());
    }

    public void fillTextEntry(String name, String password) {
        Allure.step("Fill name and password", () -> {
            clickable(id("username_edit")).sendKeys(name);
            clickable(id("password_edit")).sendKeys(password);
        });
    }

    public String textOf(String resourceId) {
        return visible(id(resourceId)).getText();
    }

    public boolean dialogGone() {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(DIALOG_TITLE));
    }
}
