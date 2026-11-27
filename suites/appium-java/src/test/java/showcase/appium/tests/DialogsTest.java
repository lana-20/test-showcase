package showcase.appium.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import showcase.appium.pages.AlertDialogsScreen;
import showcase.appium.support.BaseTest;

@Epic("ApiDemos on Android")
@Feature("Dialogs")
class DialogsTest extends BaseTest {
    private AlertDialogsScreen dialogs;

    @BeforeEach
    void openDialogs() {
        dialogs = new AlertDialogsScreen(driver).open();
    }

    @Test
    void ok_cancel_dialog_shows_both_actions_and_closes_on_ok() {
        dialogs.openOkCancelDialog();
        assertTrue(dialogs.dialogTitle().startsWith("Lorem ipsum dolor sit aie consectetur"));
        assertEquals("OK", dialogs.positiveLabel());
        assertEquals("CANCEL", dialogs.negativeLabel().toUpperCase());
        dialogs.accept();
        assertTrue(dialogs.dialogGone());
    }

    @Test
    void cancel_dismisses_the_dialog() {
        dialogs.openOkCancelDialog();
        dialogs.dismiss();
        assertTrue(dialogs.dialogGone());
    }
}
