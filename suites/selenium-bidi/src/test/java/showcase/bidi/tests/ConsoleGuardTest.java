package showcase.bidi.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import showcase.bidi.pages.BoardPage;
import showcase.bidi.pages.CatalogPage;
import showcase.bidi.pages.LoginPage;
import showcase.bidi.support.ConsoleGuard;
import showcase.bidi.support.ConsoleGuard.Problem;
import showcase.bidi.support.CrossBrowser;

/**
 * Every test in this suite runs under {@link ConsoleGuard}. These two show
 * that it passes a healthy journey and that it really fails on errors.
 */
@CrossBrowser
@Epic("Tidepool web app")
@Feature("Console and JavaScript error guard")
class ConsoleGuardTest {

    @Test
    @DisplayName("A full journey (sign in, add a task, load the catalog) logs no JavaScript errors")
    void a_clean_journey_passes_the_guard(RemoteWebDriver driver, ConsoleGuard guard) {
        new LoginPage(driver).open().submit(LoginPage.USER, LoginPage.PASSWORD);
        BoardPage board = new BoardPage(driver).waitUntilShown();
        assertEquals("Signed in as demo", board.greeting());
        board.addTask("Check the console").waitForRemaining("1 task left");
        new CatalogPage(driver).open().waitForStatus("6 devices");

        assertEquals(java.util.List.of(), guard.problems(), "log.entryAdded reported problems");
    }

    @Test
    @DisplayName("The guard catches an injected uncaught error and a console.error")
    void the_guard_fails_on_injected_errors(RemoteWebDriver driver, ConsoleGuard guard) {
        new LoginPage(driver).open();
        driver.executeScript("""
                setTimeout(() => { throw new Error("injected: uncaught"); }, 0);
                console.error("injected: console.error");
                """);

        Problem uncaught = guard.awaitProblem("the uncaught error",
                p -> p.kind().equals("uncaught error") && p.text().contains("injected: uncaught"));
        Problem logged = guard.awaitProblem("the console.error",
                p -> p.kind().equals("console.error") && p.text().contains("injected: console.error"));

        // The same check that runs after every test must fail here, and name both problems.
        AssertionError failure = assertThrows(AssertionError.class, guard::verify);
        assertTrue(failure.getMessage().contains("injected: uncaught"), failure.getMessage());
        assertTrue(failure.getMessage().contains("injected: console.error"), failure.getMessage());

        guard.acknowledge(uncaught, logged);
        assertDoesNotThrow(guard::verify);
    }
}
