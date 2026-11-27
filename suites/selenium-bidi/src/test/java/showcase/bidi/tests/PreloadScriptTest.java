package showcase.bidi.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.bidi.module.Script;
import org.openqa.selenium.remote.RemoteWebDriver;
import showcase.bidi.pages.BoardPage;
import showcase.bidi.pages.LoginPage;
import showcase.bidi.support.CrossBrowser;

/**
 * script.addPreloadScript runs a function in every new document before any
 * of the page's own scripts. The board prints today's date when app.js runs,
 * so a fixed clock installed later would be too late to change it.
 */
@CrossBrowser
@Epic("Tidepool web app")
@Feature("Preload scripts")
class PreloadScriptTest {
    private static final String FIXED_CLOCK = """
            () => {
              const fixed = Date.UTC(2030, 0, 1, 9, 0, 0);
              const RealDate = Date;
              class FixedDate extends RealDate {
                constructor(...args) { super(...(args.length ? args : [fixed])); }
                static now() { return fixed; }
              }
              window.Date = FixedDate;
            }""";
    private static final String FIXED_DAY = "Today is Tuesday, January 1, 2030";

    @BeforeEach
    void signIn(RemoteWebDriver driver) {
        LoginPage.useStoredSession(driver);
    }

    @Test
    @DisplayName("A preload script fixes the clock before the app's code runs")
    void preload_script_fixes_the_clock(RemoteWebDriver driver) {
        try (Script script = new Script(driver)) {
            Allure.step("Add a preload script that fixes Date to 2030-01-01", () -> script.addPreloadScript(FIXED_CLOCK));
            BoardPage board = new BoardPage(driver).open();
            assertEquals(FIXED_DAY, board.today());
            // It applies to every new document, not just the first.
            board.open();
            assertEquals(FIXED_DAY, board.today());
        }
    }

    @Test
    @DisplayName("Removing the preload script gives the next page load the real clock")
    void removed_preload_script_no_longer_runs(RemoteWebDriver driver) {
        try (Script script = new Script(driver)) {
            String id = script.addPreloadScript(FIXED_CLOCK);
            BoardPage board = new BoardPage(driver).open();
            assertEquals(FIXED_DAY, board.today());

            Allure.step("Remove the preload script", () -> script.removePreloadScript(id));
            String before = today();
            String shown = board.open().today();
            String after = today();   // in case the run crosses midnight UTC
            assertTrue(shown.equals(before) || shown.equals(after), "expected the real date (UTC), got " + shown);
        }
    }

    private static String today() {
        return "Today is " + LocalDate.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US));
    }
}
