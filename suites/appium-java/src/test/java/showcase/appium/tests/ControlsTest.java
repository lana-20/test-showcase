package showcase.appium.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import showcase.appium.pages.ControlsScreen;
import showcase.appium.support.BaseTest;

@Epic("ApiDemos on Android")
@Feature("Form controls")
class ControlsTest extends BaseTest {
    private ControlsScreen controls;

    @BeforeEach
    void openControls() {
        controls = new ControlsScreen(driver).open();
    }

    @Test
    void text_field_keeps_what_was_typed() {
        controls.typeText("Real devices, real signal");
        assertEquals("Real devices, real signal", controls.text());
        controls.hideKeyboardIfShown();
    }

    @Test
    void checkbox_toggles_on_and_off() {
        assertFalse(controls.isChecked(ControlsScreen.CHECK_1));
        controls.tap(ControlsScreen.CHECK_1, "Checkbox 1");
        assertTrue(controls.isChecked(ControlsScreen.CHECK_1));
        controls.tap(ControlsScreen.CHECK_1, "Checkbox 1");
        assertFalse(controls.isChecked(ControlsScreen.CHECK_1));
    }

    @Test
    void radio_buttons_are_mutually_exclusive() {
        controls.tap(ControlsScreen.RADIO_1, "RadioButton 1");
        assertTrue(controls.isChecked(ControlsScreen.RADIO_1));
        controls.tap(ControlsScreen.RADIO_2, "RadioButton 2");
        assertTrue(controls.isChecked(ControlsScreen.RADIO_2));
        assertFalse(controls.isChecked(ControlsScreen.RADIO_1));
    }

    @Test
    void toggle_button_switches_label() {
        String before = controls.label(ControlsScreen.TOGGLE_1);
        controls.tap(ControlsScreen.TOGGLE_1, "the first toggle");
        String after = controls.label(ControlsScreen.TOGGLE_1);
        assertEquals("OFF", before);
        assertEquals("ON", after);
    }

    @Test
    void disabled_save_button_is_not_enabled() {
        assertFalse(controls.isEnabled(ControlsScreen.SAVE_DISABLED));
    }
}
