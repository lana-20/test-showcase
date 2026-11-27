package showcase.appium.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.Test;
import showcase.appium.pages.LogTextBoxScreen;
import showcase.appium.support.BaseTest;

@Epic("ApiDemos on Android")
@Feature("Text")
class LogTextBoxTest extends BaseTest {

    @Test
    void each_tap_on_add_appends_one_line() {
        LogTextBoxScreen screen = new LogTextBoxScreen(driver).open();
        screen.add(3);
        assertEquals(3, LogTextBoxScreen.occurrences(screen.log(), "This is a test"));
    }
}
