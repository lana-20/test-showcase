package showcase.bidi.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.bidi.browsingcontext.BrowsingContext;
import org.openqa.selenium.bidi.browsingcontext.BrowsingContextInfo;
import org.openqa.selenium.bidi.browsingcontext.NavigationInfo;
import org.openqa.selenium.bidi.browsingcontext.NavigationResult;
import org.openqa.selenium.bidi.browsingcontext.ReadinessState;
import org.openqa.selenium.bidi.module.BrowsingContextInspector;
import org.openqa.selenium.remote.RemoteWebDriver;
import showcase.bidi.pages.BoardPage;
import showcase.bidi.pages.LoginPage;
import showcase.bidi.pages.Page;
import showcase.bidi.support.CrossBrowser;
import showcase.bidi.support.Events;

/** Navigation and tabs through browsingContext commands, with the browser's own events as the signal. */
@CrossBrowser
@Epic("Tidepool web app")
@Feature("Browsing contexts and navigation events")
class BrowsingContextEventsTest {

    @Test
    @DisplayName("A signed-out deep link redirects to sign-in, and sign-in lands on the board: both seen as load events")
    void redirects_are_seen_as_load_events(RemoteWebDriver driver, BrowsingContext tab) {
        try (BrowsingContextInspector inspector = new BrowsingContextInspector(driver)) {
            CompletableFuture<NavigationInfo> signInLoaded = new CompletableFuture<>();
            CompletableFuture<NavigationInfo> boardLoaded = new CompletableFuture<>();
            CompletableFuture<String> deepLink = new CompletableFuture<>();
            inspector.onBrowsingContextLoaded(nav -> {
                if (nav.getUrl().endsWith("/index.html?next=board.html")) {
                    signInLoaded.complete(nav);
                } else if (nav.getUrl().endsWith("/board.html") && deepLink.isDone()
                        && !deepLink.join().equals(nav.getNavigationId())) {
                    boardLoaded.complete(nav);
                }
            });

            // ReadinessState.NONE: the app replaces this navigation with a redirect, so waiting for
            // it to complete would fail ("canceled by concurrent navigation"). The load events say where it went.
            NavigationResult first = Allure.step("Navigate to board.html while signed out", () ->
                    tab.navigate(Page.url("board.html"), ReadinessState.NONE));
            deepLink.complete(first.getNavigationId());
            NavigationInfo signIn = Events.await(signInLoaded, "load of index.html?next=board.html");
            assertEquals(tab.getId(), signIn.getBrowsingContextId());

            new LoginPage(driver).submit(LoginPage.USER, LoginPage.PASSWORD);
            NavigationInfo board = Events.await(boardLoaded, "load of board.html after sign-in");
            assertNotEquals(first.getNavigationId(), board.getNavigationId());
            assertEquals("Signed in as demo", new BoardPage(driver).waitUntilShown().greeting());
        }
    }

    @Test
    @DisplayName("A new tab is announced by contextCreated and contextDestroyed events")
    void new_tab_lifecycle_events(RemoteWebDriver driver, BrowsingContext tab) {
        try (BrowsingContextInspector inspector = new BrowsingContextInspector(driver)) {
            CompletableFuture<BrowsingContextInfo> created = new CompletableFuture<>();
            CompletableFuture<BrowsingContextInfo> destroyed = new CompletableFuture<>();
            inspector.onBrowsingContextCreated(info -> {
                if (!info.getId().equals(tab.getId()) && info.getParentBrowsingContext() == null) created.complete(info);
            });
            inspector.onBrowsingContextDestroyed(destroyed::complete);

            BrowsingContext second = Allure.step("Open a second tab", () -> new BrowsingContext(driver, WindowType.TAB));
            assertEquals(second.getId(), Events.await(created, "contextCreated for the new tab").getId());

            second.navigate(Page.url("index.html"), ReadinessState.COMPLETE);
            List<BrowsingContextInfo> tabs = tab.getTopLevelContexts();
            assertEquals(2, tabs.size(), "top-level contexts: " + tabs);
            assertTrue(tabs.stream().anyMatch(t -> t.getId().equals(second.getId()) && t.getUrl().endsWith("/index.html")),
                    "the new tab shows index.html: " + tabs);

            Allure.step("Close the second tab", second::close);
            assertEquals(second.getId(), Events.await(destroyed, "contextDestroyed for the new tab").getId());
        }
    }
}
