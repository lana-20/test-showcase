package showcase.appium.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public final class HomeScreen extends Screen {
    private static final By ITEMS = AppiumBy.id("android:id/text1");

    public HomeScreen(AndroidDriver driver) {
        super(driver);
    }

    public HomeScreen open() {
        startActivity(".ApiDemos");
        visible(AppiumBy.androidUIAutomator("new UiSelector().text(\"Animation\")"));
        return this;
    }

    public List<String> visibleCategories() {
        return Allure.step("Read the visible categories", () ->
                driver.findElements(ITEMS).stream().map(WebElement::getText).toList());
    }

    public void navigate(String... path) {
        for (String item : path) {
            tapListItem(item);
        }
    }
}
