package showcase.appium.pages;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Text > LogTextBox. */
public final class LogTextBoxScreen extends Screen {
    private static final By ADD = id("add");
    private static final By LOG = id("text");

    public LogTextBoxScreen(AndroidDriver driver) {
        super(driver);
    }

    public LogTextBoxScreen open() {
        startActivity(".text.LogTextBox1");
        visible(ADD);
        return this;
    }

    public void add(int times) {
        for (int i = 1; i <= times; i++) {
            int n = i;
            Allure.step("Tap Add (" + n + ")", () -> {
                clickable(ADD).click();
                wait.until(d -> occurrences(log(), "This is a test") >= n);
            });
        }
    }

    public String log() {
        return visible(LOG).getText();
    }

    public void waitForLogToContain(String text) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(LOG, text));
    }

    public static int occurrences(String haystack, String needle) {
        int count = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }
}
