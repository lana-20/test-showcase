package showcase.bidi.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.bidi.browsingcontext.BrowsingContext;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.bidi.network.AddInterceptParameters;
import org.openqa.selenium.bidi.network.BeforeRequestSent;
import org.openqa.selenium.bidi.network.BytesValue;
import org.openqa.selenium.bidi.network.ContinueRequestParameters;
import org.openqa.selenium.bidi.network.Header;
import org.openqa.selenium.bidi.network.InterceptPhase;
import org.openqa.selenium.bidi.network.ProvideResponseParameters;
import org.openqa.selenium.bidi.network.RequestData;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import showcase.bidi.pages.CatalogPage;
import showcase.bidi.pages.LoginPage;
import showcase.bidi.pages.Page;
import showcase.bidi.support.Attachments;
import showcase.bidi.support.Config;
import showcase.bidi.support.CrossBrowser;
import showcase.bidi.support.Events;

/**
 * The catalog loads devices.json after the page has loaded. BiDi network
 * interception pauses that request in the browser, so a test can answer it,
 * fail it or hold it, and observe exactly what the app sent.
 */
@CrossBrowser
@Epic("Tidepool web app")
@Feature("Network interception")
class NetworkInterceptionTest {
    private static final String DEVICES_URL = Page.url(CatalogPage.DEVICES_PATH);
    private static final String MOCK_CATALOG = """
            [{"id": "mock-1", "name": "Mocked Pixel", "platform": "android", "os": "Android 99"},
             {"id": "mock-2", "name": "Mocked iPhone", "platform": "ios", "os": "iOS 99"}]""";

    @BeforeEach
    void signIn(RemoteWebDriver driver) {
        LoginPage.useStoredSession(driver);
    }

    @Test
    @DisplayName("A mocked devices.json response is what the catalog renders")
    void mocked_catalog_response(RemoteWebDriver driver) {
        try (Network network = new Network(driver)) {
            String intercept = interceptCatalog(network);
            network.onBeforeRequestSent(e -> {
                if (pausedBy(e, intercept)) {
                    network.provideResponse(json(e, 200, "OK", MOCK_CATALOG));
                }
            });

            CatalogPage catalog = new CatalogPage(driver).open().waitForStatus("2 devices");
            assertEquals(List.of("Mocked Pixel · Android 99", "Mocked iPhone · iOS 99"), catalog.deviceNames());
        }
    }

    @Test
    @DisplayName("A 500 from devices.json shows the app's error state")
    void server_error_shows_error_state(RemoteWebDriver driver) {
        try (Network network = new Network(driver)) {
            String intercept = interceptCatalog(network);
            network.onBeforeRequestSent(e -> {
                if (pausedBy(e, intercept)) {
                    network.provideResponse(json(e, 500, "Internal Server Error", "{\"error\": \"catalog unavailable\"}"));
                }
            });

            CatalogPage catalog = new CatalogPage(driver).open().waitForStatus("Could not load devices.");
            assertEquals(List.of(), catalog.deviceNames());
        }
    }

    @Test
    @DisplayName("While devices.json is held, the catalog shows its loading state")
    void held_response_shows_loading_state(RemoteWebDriver driver, BrowsingContext tab) {
        try (Network network = new Network(driver)) {
            String intercept = interceptCatalog(network);
            CompletableFuture<String> held = new CompletableFuture<>();
            network.onBeforeRequestSent(e -> {
                if (pausedBy(e, intercept)) {
                    held.complete(e.getRequest().getRequestId());   // and do not continue it yet
                }
            });

            CatalogPage catalog = new CatalogPage(driver).open();
            String requestId = Events.await(held, "beforeRequestSent for devices.json (paused)");

            // The request cannot finish while it is paused, so this is not a race.
            assertEquals("Loading…", catalog.status());
            assertEquals(List.of(), catalog.deviceNames());
            Attachments.png("catalog while devices.json is held", tab.captureScreenshot());

            Allure.step("Release the request", () -> network.continueRequest(new ContinueRequestParameters(requestId)));
            catalog.waitForStatus("6 devices");
        }
    }

    @Test
    @DisplayName("The catalog sends one authorized GET for devices.json, and filtering does not refetch")
    void outgoing_request_is_authorized_and_not_duplicated(RemoteWebDriver driver) {
        try (Network network = new Network(driver)) {
            List<RequestData> sent = new CopyOnWriteArrayList<>();
            List<String> completed = new CopyOnWriteArrayList<>();
            network.onBeforeRequestSent(e -> {
                if (e.getRequest().getUrl().equals(DEVICES_URL)) sent.add(e.getRequest());
            });
            network.onResponseCompleted(e -> {
                if (e.getRequest().getUrl().equals(DEVICES_URL)) completed.add(e.getRequest().getRequestId());
            });

            CatalogPage catalog = new CatalogPage(driver).open().waitForStatus("6 devices");
            catalog.search("pixel").waitForStatus("2 devices");
            catalog.platform("ios").waitForStatus("No devices match.");
            // The responseCompleted event for the load can trail the DOM update slightly.
            new FluentWait<>(completed).withTimeout(Config.timeout()).pollingEvery(Duration.ofMillis(50))
                    .withMessage("no responseCompleted event for devices.json").until(list -> !list.isEmpty());

            Allure.addAttachment("requests to devices.json", sent.toString());
            assertEquals(1, sent.size(), "requests to devices.json");
            RequestData request = sent.get(0);
            assertEquals("GET", request.getMethod());
            String authorization = request.getHeaders().stream()
                    .filter(h -> h.getName().equalsIgnoreCase("authorization"))
                    .map(h -> h.getValue().getValue())
                    .findFirst()
                    .orElse(null);
            assertEquals("Bearer tp-demo", authorization, "Authorization header");
            assertTrue(completed.contains(request.getRequestId()), "the request that completed is the one sent");
        }
    }

    private static String interceptCatalog(Network network) {
        return Allure.step("Intercept " + CatalogPage.DEVICES_PATH + " before it is sent", () ->
                network.addIntercept(new AddInterceptParameters(InterceptPhase.BEFORE_REQUEST_SENT)
                        .urlStringPattern(DEVICES_URL)));
    }

    private static boolean pausedBy(BeforeRequestSent event, String intercept) {
        return event.isBlocked() && event.getIntercepts() != null && event.getIntercepts().contains(intercept);
    }

    private static ProvideResponseParameters json(BeforeRequestSent event, int status, String reason, String body) {
        // Runs on the BiDi event thread, outside the test's Allure context: no steps here.
        return new ProvideResponseParameters(event.getRequest().getRequestId())
                .statusCode(status)
                .reasonPhrase(reason)
                .headers(List.of(new Header("Content-Type", new BytesValue(BytesValue.Type.STRING, "application/json"))))
                .body(new BytesValue(BytesValue.Type.STRING, body));
    }
}
