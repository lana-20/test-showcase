package showcase.appium.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.util.List;
import org.junit.jupiter.api.Test;
import showcase.appium.pages.ControlsScreen;
import showcase.appium.pages.HomeScreen;
import showcase.appium.support.BaseTest;

@Epic("ApiDemos on Android")
@Feature("Navigation")
class NavigationTest extends BaseTest {

    @Test
    void home_lists_the_demo_categories_in_order() {
        List<String> categories = new HomeScreen(driver).open().visibleCategories();
        assertTrue(categories.size() >= 5, "expected a list of categories, got " + categories);
        assertEquals(List.of("Access'ibility", "Accessibility", "Animation", "App", "Content"),
                categories.subList(0, 5));
    }

    @Test
    void scrolling_the_list_reaches_views_controls_light_theme() {
        HomeScreen home = new HomeScreen(driver).open();
        home.navigate("Views", "Controls", "1. Light Theme");
        ControlsScreen controls = new ControlsScreen(driver).waitUntilShown();
        assertEquals("Save", controls.label(ControlsScreen.byText("Save")));
    }
}
